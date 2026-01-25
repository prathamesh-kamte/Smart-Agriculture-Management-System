-- First, create the user_profiles table
CREATE TABLE public.user_profiles (
  id UUID NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
  user_id UUID NOT NULL UNIQUE REFERENCES auth.users(id) ON DELETE CASCADE,
  user_type TEXT NOT NULL CHECK (user_type IN ('student', 'industry')),
  name TEXT,
  company_name TEXT,
  qualification TEXT,
  location TEXT,
  skills JSONB DEFAULT '[]'::jsonb,
  sector_interest TEXT,
  category TEXT,
  role TEXT,
  required_skills JSONB DEFAULT '[]'::jsonb,
  internship_capacity SMALLINT,
  sector TEXT,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- Enable RLS on user_profiles
ALTER TABLE public.user_profiles ENABLE ROW LEVEL SECURITY;

-- Create policies for user_profiles
CREATE POLICY "Users can view their own profile" 
ON public.user_profiles 
FOR SELECT 
USING (auth.uid() = user_id);

CREATE POLICY "Users can insert their own profile" 
ON public.user_profiles 
FOR INSERT 
WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update their own profile" 
ON public.user_profiles 
FOR UPDATE 
USING (auth.uid() = user_id);

-- Allow students to view industry profiles for matching
CREATE POLICY "Students can view industry profiles" 
ON public.user_profiles 
FOR SELECT 
USING (
  user_type = 'industry' AND 
  EXISTS (
    SELECT 1 FROM public.user_profiles up 
    WHERE up.user_id = auth.uid() AND up.user_type = 'student'
  )
);

-- Allow industries to view student profiles for matching
CREATE POLICY "Industries can view student profiles" 
ON public.user_profiles 
FOR SELECT 
USING (
  user_type = 'student' AND 
  EXISTS (
    SELECT 1 FROM public.user_profiles up 
    WHERE up.user_id = auth.uid() AND up.user_type = 'industry'
  )
);

-- Update the matches table to reference user_profiles
ALTER TABLE public.matches DROP CONSTRAINT IF EXISTS matches_student_id_fkey;
ALTER TABLE public.matches DROP CONSTRAINT IF EXISTS matches_industry_id_fkey;

-- Add proper foreign keys
ALTER TABLE public.matches 
ADD CONSTRAINT matches_student_id_fkey 
FOREIGN KEY (student_id) REFERENCES public.user_profiles(user_id) ON DELETE CASCADE;

ALTER TABLE public.matches 
ADD CONSTRAINT matches_industry_id_fkey 
FOREIGN KEY (industry_id) REFERENCES public.user_profiles(user_id) ON DELETE CASCADE;

-- Update RLS policies for matches to be secure
DROP POLICY IF EXISTS "Enable select for matches" ON public.matches;
DROP POLICY IF EXISTS "Enable insert for matches" ON public.matches;
DROP POLICY IF EXISTS "Enable update for matches" ON public.matches;

CREATE POLICY "Authenticated users can view their matches" 
ON public.matches 
FOR SELECT 
USING (
  auth.uid() = student_id OR 
  auth.uid() = industry_id
);

CREATE POLICY "System can create matches" 
ON public.matches 
FOR INSERT 
WITH CHECK (true);

CREATE POLICY "Users can update match status" 
ON public.matches 
FOR UPDATE 
USING (
  auth.uid() = student_id OR 
  auth.uid() = industry_id
);

-- Remove password columns from existing tables (but keep tables for data migration)
ALTER TABLE public.students DROP COLUMN IF EXISTS password;
ALTER TABLE public.industries DROP COLUMN IF EXISTS password;

-- Update RLS policies for legacy tables to be read-only for migration purposes
DROP POLICY IF EXISTS "Enable select for all users" ON public.students;
DROP POLICY IF EXISTS "Enable insert for public registration" ON public.students;
DROP POLICY IF EXISTS "Enable update for users based on email" ON public.students;

CREATE POLICY "Authenticated users can view students for migration" 
ON public.students 
FOR SELECT 
USING (auth.uid() IS NOT NULL);

DROP POLICY IF EXISTS "Enable select for all users" ON public.industries;
DROP POLICY IF EXISTS "Enable insert for public registration" ON public.industries;
DROP POLICY IF EXISTS "Enable update for users based on email" ON public.industries;

CREATE POLICY "Authenticated users can view industries for migration" 
ON public.industries 
FOR SELECT 
USING (auth.uid() IS NOT NULL);

-- Create function to update timestamps
CREATE OR REPLACE FUNCTION public.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SET search_path = public;

-- Create trigger for automatic timestamp updates
CREATE TRIGGER update_user_profiles_updated_at
BEFORE UPDATE ON public.user_profiles
FOR EACH ROW
EXECUTE FUNCTION public.update_updated_at_column();