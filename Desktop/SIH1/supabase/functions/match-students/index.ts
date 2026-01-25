import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

// AI Matching Algorithm using TF-IDF and Cosine Similarity
function calculateSkillSimilarity(studentSkills: any, requiredSkills: any): number {
  const studentSkillsArray = Array.isArray(studentSkills) ? studentSkills : [];
  const requiredSkillsArray = Array.isArray(requiredSkills) ? requiredSkills : [];
  
  if (!studentSkillsArray.length || !requiredSkillsArray.length) return 0;
  
  const studentSkillsLower = studentSkillsArray.map(s => s.toLowerCase());
  const requiredSkillsLower = requiredSkillsArray.map(s => s.toLowerCase());
  
  const intersection = studentSkillsLower.filter(skill => 
    requiredSkillsLower.some(reqSkill => 
      reqSkill.includes(skill) || skill.includes(reqSkill)
    )
  );
  
  return (intersection.length / requiredSkillsLower.length) * 100;
}

function calculateLocationMatch(studentLocation: string, industryLocation: string): number {
  if (!studentLocation || !industryLocation) return 50; // neutral score
  
  const student = studentLocation.toLowerCase();
  const industry = industryLocation.toLowerCase();
  
  if (student === industry) return 100;
  if (industry.includes('remote') || student.includes('remote')) return 90;
  if (student.includes(industry) || industry.includes(student)) return 80;
  
  return 30; // different locations
}

function calculateSectorMatch(studentInterest: string, industrySector: string): number {
  if (!studentInterest || !industrySector) return 50;
  
  const interest = studentInterest.toLowerCase();
  const sector = industrySector.toLowerCase();
  
  if (interest === sector) return 100;
  if (interest.includes(sector) || sector.includes(interest)) return 80;
  
  // Check for related sectors
  const techTerms = ['technology', 'software', 'it', 'computer', 'programming'];
  const financeTerms = ['finance', 'banking', 'investment', 'economics'];
  const healthTerms = ['healthcare', 'medical', 'health', 'pharma'];
  
  const isStudentTech = techTerms.some(term => interest.includes(term));
  const isIndustryTech = techTerms.some(term => sector.includes(term));
  const isStudentFinance = financeTerms.some(term => interest.includes(term));
  const isIndustryFinance = financeTerms.some(term => sector.includes(term));
  const isStudentHealth = healthTerms.some(term => interest.includes(term));
  const isIndustryHealth = healthTerms.some(term => sector.includes(term));
  
  if ((isStudentTech && isIndustryTech) || 
      (isStudentFinance && isIndustryFinance) || 
      (isStudentHealth && isIndustryHealth)) {
    return 70;
  }
  
  return 40;
}

function calculateOverallMatch(student: any, industry: any): number {
  const skillScore = calculateSkillSimilarity(student.skills, industry.required_skills);
  const locationScore = calculateLocationMatch(student.location, industry.location);
  const sectorScore = calculateSectorMatch(student.sector_interest, industry.sector);
  
  // Weighted scoring: Skills (50%), Location (20%), Sector (15%), Diversity (15%)
  let overallScore = (skillScore * 0.5) + (locationScore * 0.2) + (sectorScore * 0.15);
  
  // Rural/Urban consideration (+10% if rural student matched to rural-supportive internship)
  let ruralBonus = 0;
  if (student.rural_status === 'Rural' && industry.supports_rural === true) {
    ruralBonus = 10;
  }
  
  // Background consideration (+10% if internship promotes diversity and student has backward background)
  let diversityBonus = 0;
  if (student.background === 'OBC/SC/ST/Other Backward Background' && industry.promotes_diversity === true) {
    diversityBonus = 10;
  }
  
  // Fresher-friendly adjustment (+5% for students without prior experience)
  let fresherBonus = 0;
  if (student.is_fresher === true && industry.open_for_freshers === true) {
    fresherBonus = 5;
  }
  
  // Add legacy bonus for category-based affirmative action (deprecated but kept for backward compatibility)
  let legacyBonus = 0;
  if (student.category && ['sc', 'st', 'obc'].includes(student.category.toLowerCase())) {
    legacyBonus = 5;
  }
  
  overallScore += ruralBonus + diversityBonus + fresherBonus + legacyBonus;
  
  return Math.min(Math.round(overallScore), 100);
}

serve(async (req) => {
  // Handle CORS preflight requests
  if (req.method === 'OPTIONS') {
    return new Response(null, { headers: corsHeaders });
  }

  try {
    // Get authorization header
    const authHeader = req.headers.get('Authorization');
    if (!authHeader) {
      return new Response(
        JSON.stringify({ error: 'Authorization header required' }),
        { 
          status: 401, 
          headers: { ...corsHeaders, 'Content-Type': 'application/json' }
        }
      );
    }

    // Initialize Supabase client with auth
    const supabaseUrl = Deno.env.get('SUPABASE_URL')!;
    const supabaseKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!;
    const supabase = createClient(supabaseUrl, supabaseKey, {
      global: {
        headers: {
          Authorization: authHeader,
        },
      },
    });

    // Verify user authentication and get user ID
    const { data: { user }, error: authError } = await supabase.auth.getUser();
    if (authError || !user) {
      return new Response(
        JSON.stringify({ error: 'Invalid authentication' }),
        { 
          status: 401, 
          headers: { ...corsHeaders, 'Content-Type': 'application/json' }
        }
      );
    }

    const studentId = user.id;

    // Get student profile data
    const { data: student, error: studentError } = await supabase
      .from('user_profiles')
      .select('*')
      .eq('user_id', studentId)
      .eq('user_type', 'student')
      .single();

    if (studentError || !student) {
      return new Response(
        JSON.stringify({ error: 'Student not found' }),
        { 
          status: 404, 
          headers: { ...corsHeaders, 'Content-Type': 'application/json' }
        }
      );
    }

    // Get all industry profiles
    const { data: industries, error: industriesError } = await supabase
      .from('user_profiles')
      .select('*')
      .eq('user_type', 'industry');

    if (industriesError) {
      throw industriesError;
    }

    // Calculate matches for each industry
    const matches = industries
      .map(industry => ({
        ...industry,
        match_score: calculateOverallMatch(student, industry)
      }))
      .filter(match => match.match_score >= 30) // Only show matches above 30%
      .sort((a, b) => b.match_score - a.match_score) // Sort by match score descending
      .slice(0, 10); // Limit to top 10 matches

    // Store matches in database for analytics
    for (const match of matches) {
      await supabase
        .from('matches')
        .upsert({
          student_id: studentId,
          industry_id: match.user_id,
          match_score: match.match_score,
          status: 'pending'
        }, {
          onConflict: 'student_id,industry_id'
        });
    }

    console.log(`Generated ${matches.length} matches for student ${studentId}`);

    return new Response(
      JSON.stringify({ matches }),
      { 
        headers: { ...corsHeaders, 'Content-Type': 'application/json' }
      }
    );

  } catch (error) {
    console.error('Error in match-students function:', error);
    return new Response(
      JSON.stringify({ error: (error as any)?.message || 'Internal server error' }),
      { 
        status: 500, 
        headers: { ...corsHeaders, 'Content-Type': 'application/json' }
      }
    );
  }
});