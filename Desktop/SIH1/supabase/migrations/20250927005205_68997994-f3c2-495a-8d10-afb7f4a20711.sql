-- Create trigger to automatically create user profile when auth user is created
-- This ensures the user_id foreign key constraint is always satisfied

-- First, create a function to handle new user creation
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
  -- Insert a basic profile for the new user
  -- Additional profile data will be updated via the frontend after signup
  INSERT INTO public.user_profiles (
    user_id,
    user_type,
    created_at,
    updated_at
  ) VALUES (
    NEW.id,
    COALESCE(NEW.raw_user_meta_data ->> 'user_type', 'student'),
    now(),
    now()
  );
  
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

-- Create trigger to fire when a new user is created in auth.users
DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- Also create a function to update user profiles safely
CREATE OR REPLACE FUNCTION public.update_user_profile(
  p_user_id UUID,
  p_user_type TEXT DEFAULT NULL,
  p_name TEXT DEFAULT NULL,
  p_company_name TEXT DEFAULT NULL,
  p_qualification TEXT DEFAULT NULL,
  p_location TEXT DEFAULT NULL,
  p_skills JSONB DEFAULT NULL,
  p_sector_interest TEXT DEFAULT NULL,
  p_category TEXT DEFAULT NULL,
  p_role TEXT DEFAULT NULL,
  p_required_skills JSONB DEFAULT NULL,
  p_internship_capacity SMALLINT DEFAULT NULL,
  p_sector TEXT DEFAULT NULL,
  p_experience_level TEXT DEFAULT NULL,
  p_portfolio_url TEXT DEFAULT NULL,
  p_linkedin_url TEXT DEFAULT NULL
)
RETURNS VOID AS $$
BEGIN
  UPDATE public.user_profiles SET
    user_type = COALESCE(p_user_type, user_type),
    name = COALESCE(p_name, name),
    company_name = COALESCE(p_company_name, company_name),
    qualification = COALESCE(p_qualification, qualification),
    location = COALESCE(p_location, location),
    skills = COALESCE(p_skills, skills),
    sector_interest = COALESCE(p_sector_interest, sector_interest),
    category = COALESCE(p_category, category),
    role = COALESCE(p_role, role),
    required_skills = COALESCE(p_required_skills, required_skills),
    internship_capacity = COALESCE(p_internship_capacity, internship_capacity),
    sector = COALESCE(p_sector, sector),
    experience_level = COALESCE(p_experience_level, experience_level),
    portfolio_url = COALESCE(p_portfolio_url, portfolio_url),
    linkedin_url = COALESCE(p_linkedin_url, linkedin_url),
    updated_at = now()
  WHERE user_id = p_user_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;