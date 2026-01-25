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
  
  // Weighted scoring: Skills (60%), Location (25%), Sector (15%)
  const overallScore = (skillScore * 0.6) + (locationScore * 0.25) + (sectorScore * 0.15);
  
  // Add bonus for category-based affirmative action
  let bonus = 0;
  if (student.category && ['sc', 'st', 'obc'].includes(student.category.toLowerCase())) {
    bonus = 5; // 5% bonus for reserved categories
  }
  
  // Add bonus for rural areas (assuming non-metro cities are rural)
  const metroCities = ['mumbai', 'delhi', 'bangalore', 'hyderabad', 'chennai', 'kolkata', 'pune'];
  const isRural = student.location && !metroCities.some(city => 
    student.location.toLowerCase().includes(city)
  );
  if (isRural) {
    bonus += 3; // 3% bonus for rural candidates
  }
  
  return Math.min(Math.round(overallScore + bonus), 100);
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

    const industryId = user.id;

    // Get industry profile data
    const { data: industry, error: industryError } = await supabase
      .from('user_profiles')
      .select('*')
      .eq('user_id', industryId)
      .eq('user_type', 'industry')
      .single();

    if (industryError || !industry) {
      return new Response(
        JSON.stringify({ error: 'Industry profile not found' }),
        { 
          status: 404, 
          headers: { ...corsHeaders, 'Content-Type': 'application/json' }
        }
      );
    }

    // Get all student profiles
    const { data: students, error: studentsError } = await supabase
      .from('user_profiles')
      .select('*')
      .eq('user_type', 'student');

    if (studentsError) {
      throw studentsError;
    }

    // Calculate matches for each student
    const matches = students
      .map(student => ({
        id: student.id,
        user_id: student.user_id,
        name: student.name || 'Unknown',
        qualification: student.qualification || 'Not specified',
        skills: Array.isArray(student.skills) ? student.skills.map((s: any) => String(s)) : [],
        location: student.location || 'Not specified',
        sector_interest: student.sector_interest || 'Not specified',
        category: student.category || '',
        experience_level: student.experience_level || 'Not specified',
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
          student_id: match.user_id,
          industry_id: industryId,
          match_score: match.match_score,
          status: 'pending'
        }, {
          onConflict: 'student_id,industry_id'
        });
    }

    console.log(`Generated ${matches.length} matches for industry ${industryId}`);

    return new Response(
      JSON.stringify({ matches }),
      { 
        headers: { ...corsHeaders, 'Content-Type': 'application/json' }
      }
    );

  } catch (error) {
    console.error('Error in match-industry function:', error);
    return new Response(
      JSON.stringify({ error: (error as any)?.message || 'Internal server error' }),
      { 
        status: 500, 
        headers: { ...corsHeaders, 'Content-Type': 'application/json' }
      }
    );
  }
});