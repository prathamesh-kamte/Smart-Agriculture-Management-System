import "https://deno.land/x/xhr@0.1.0/mod.ts";
import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2.57.4';

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
};

const openAIApiKey = Deno.env.get('OPENAI_API_KEY');
const supabaseUrl = Deno.env.get('SUPABASE_URL')!;
const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!;

serve(async (req) => {
  if (req.method === 'OPTIONS') {
    return new Response(null, { headers: corsHeaders });
  }

  try {
    const supabase = createClient(supabaseUrl, supabaseServiceKey);
    
    const { studentId, limit = 10 } = await req.json();
    console.log('Matching student:', studentId);

    if (!studentId) {
      throw new Error('Student ID is required');
    }

    // Get student profile
    const { data: student, error: studentError } = await supabase
      .from('user_profiles')
      .select('*')
      .eq('user_id', studentId)
      .eq('user_type', 'student')
      .single();

    if (studentError || !student) {
      throw new Error('Student not found');
    }

    // Get all active internships
    const { data: internships, error: internshipsError } = await supabase
      .from('internships')
      .select(`
        *,
        company:user_profiles!internships_industry_id_fkey (
          company_name,
          name,
          location
        )
      `)
      .eq('is_active', true);

    if (internshipsError) {
      throw new Error('Failed to fetch internships');
    }

    // Calculate match scores using AI embeddings
    const matches = [];
    
    for (const internship of internships || []) {
      let matchScore = 0;
      let reasons = [];
      let skillMatchPercentage = 0;
      let locationMatched = false;
      let ruralPriority = false;
      let fresherFriendly = false;

      // Skill matching using AI embeddings
      const studentSkills = Array.isArray(student.skills) ? student.skills : [];
      const requiredSkills = Array.isArray(internship.required_skills) ? internship.required_skills : [];

      if (studentSkills.length > 0 && requiredSkills.length > 0) {
        const skillMatch = await calculateSkillSimilarity(studentSkills, requiredSkills);
        skillMatchPercentage = Math.round(skillMatch * 100);
        matchScore += skillMatch * 0.5; // 50% weight for skills
        if (skillMatch > 0.7) reasons.push('Strong skill match');
      }

      // Location matching
      if (student.location && internship.location) {
        locationMatched = student.location.toLowerCase().includes(internship.location.toLowerCase()) ||
                          internship.location.toLowerCase().includes(student.location.toLowerCase()) ||
                          internship.location.toLowerCase().includes('remote');
        if (locationMatched) {
          matchScore += 0.2; // 20% weight for location
          reasons.push('Location preference match');
        }
      }

      // Sector matching
      if (student.sector_interest && internship.sector) {
        const sectorMatch = student.sector_interest.toLowerCase().includes(internship.sector.toLowerCase()) ||
                          internship.sector.toLowerCase().includes(student.sector_interest.toLowerCase());
        if (sectorMatch) {
          matchScore += 0.15; // 15% weight for sector
          reasons.push('Sector interest alignment');
        }
      }

      // Rural/Urban consideration
      if (student.rural_status === 'Rural' && internship.supports_rural === true) {
        matchScore += 0.1; // 10% bonus
        ruralPriority = true;
        reasons.push('Rural candidate priority');
      }

      // Background consideration (diversity)
      if (student.background === 'OBC/SC/ST/Other Backward Background' && internship.promotes_diversity === true) {
        matchScore += 0.1; // 10% bonus
        reasons.push('Diversity program match');
      }

      // Fresher-friendly adjustment
      if (student.is_fresher === true && internship.open_for_freshers === true) {
        matchScore += 0.05; // 5% bonus
        fresherFriendly = true;
        reasons.push('Open for freshers');
      }

      // Experience level matching (legacy)
      if (student.experience_level) {
        matchScore += 0.05; // 5% weight for having experience level
      }

      // Normalize score to 0-100
      matchScore = Math.min(matchScore * 100, 100);

      if (matchScore > 30) { // Only include matches above 30%
        matches.push({
          internship_id: internship.id,
          student_id: studentId,
          match_score: Math.round(matchScore),
          skill_match: skillMatchPercentage,
          location_match: locationMatched,
          rural_priority: ruralPriority,
          fresher_friendly: fresherFriendly,
          internship: {
            ...internship,
            company: internship.company
          },
          match_reasons: reasons
        });
      }
    }

    // Sort by match score and limit results
    matches.sort((a, b) => b.match_score - a.match_score);
    const topMatches = matches.slice(0, limit);

    // Store matches in database
    for (const match of topMatches) {
      await supabase
        .from('matches')
        .upsert({
          student_id: studentId,
          internship_id: match.internship_id,
          match_score: match.match_score,
          skill_match: match.skill_match,
          location_match: match.location_match,
          rural_priority: match.rural_priority,
          fresher_friendly: match.fresher_friendly,
          match_reasons: match.match_reasons,
          status: 'matched',
          created_at: new Date().toISOString()
        }, {
          onConflict: 'student_id,internship_id'
        });
    }

    console.log(`Found ${topMatches.length} matches for student ${studentId}`);

    return new Response(JSON.stringify({ 
      matches: topMatches,
      total: topMatches.length
    }), {
      headers: { ...corsHeaders, 'Content-Type': 'application/json' },
    });

  } catch (error: any) {
    console.error('Error in ai-match-students function:', error);
    return new Response(JSON.stringify({ 
      error: error?.message || 'Internal server error',
      matches: [],
      total: 0
    }), {
      status: 500,
      headers: { ...corsHeaders, 'Content-Type': 'application/json' },
    });
  }
});

async function calculateSkillSimilarity(studentSkills: string[], requiredSkills: string[]): Promise<number> {
  try {
    if (!openAIApiKey) {
      // Fallback to simple text matching if no OpenAI key
      const matchCount = studentSkills.filter(skill => 
        requiredSkills.some(reqSkill => 
          skill.toLowerCase().includes(reqSkill.toLowerCase()) ||
          reqSkill.toLowerCase().includes(skill.toLowerCase())
        )
      ).length;
      return matchCount / Math.max(requiredSkills.length, 1);
    }

    // Get embeddings for student skills
    const studentEmbedding = await getEmbedding(studentSkills.join(', '));
    const requiredEmbedding = await getEmbedding(requiredSkills.join(', '));

    // Calculate cosine similarity
    const similarity = cosineSimilarity(studentEmbedding, requiredEmbedding);
    return Math.max(0, Math.min(1, similarity));
    
  } catch (error: any) {
    console.error('Error calculating skill similarity:', error);
    // Fallback to text matching
    const matchCount = studentSkills.filter(skill => 
      requiredSkills.some(reqSkill => 
        skill.toLowerCase().includes(reqSkill.toLowerCase()) ||
        reqSkill.toLowerCase().includes(skill.toLowerCase())
      )
    ).length;
    return matchCount / Math.max(requiredSkills.length, 1);
  }
}

async function getEmbedding(text: string): Promise<number[]> {
  const response = await fetch('https://api.openai.com/v1/embeddings', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${openAIApiKey}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      model: 'text-embedding-3-small',
      input: text,
    }),
  });

  const data = await response.json();
  return data.data[0].embedding;
}

function cosineSimilarity(vecA: number[], vecB: number[]): number {
  const dotProduct = vecA.reduce((sum, a, i) => sum + a * vecB[i], 0);
  const magnitudeA = Math.sqrt(vecA.reduce((sum, a) => sum + a * a, 0));
  const magnitudeB = Math.sqrt(vecB.reduce((sum, b) => sum + b * b, 0));
  
  if (magnitudeA === 0 || magnitudeB === 0) return 0;
  return dotProduct / (magnitudeA * magnitudeB);
}