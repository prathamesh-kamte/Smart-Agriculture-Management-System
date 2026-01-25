-- Drop existing problematic policies
DROP POLICY IF EXISTS "Users can insert their own profile" ON public.user_profiles;
DROP POLICY IF EXISTS "Users can update their own profile" ON public.user_profiles;
DROP POLICY IF EXISTS "Users can view their own profile" ON public.user_profiles;
DROP POLICY IF EXISTS "Industries can view student profiles" ON public.user_profiles;
DROP POLICY IF EXISTS "Students can view industry profiles" ON public.user_profiles;

-- Create new RLS policies for user_profiles
-- Allow profile creation during signup (before full authentication)
CREATE POLICY "Allow profile creation during signup" 
ON public.user_profiles 
FOR INSERT 
WITH CHECK (true);

-- Users can view their own profile
CREATE POLICY "Users can view their own profile" 
ON public.user_profiles 
FOR SELECT 
USING (auth.uid() = user_id);

-- Users can update their own profile
CREATE POLICY "Users can update their own profile" 
ON public.user_profiles 
FOR UPDATE 
USING (auth.uid() = user_id);

-- Students can view industry profiles for matching
CREATE POLICY "Students can view industry profiles" 
ON public.user_profiles 
FOR SELECT 
USING (
  user_type = 'industry' 
  AND EXISTS (
    SELECT 1 FROM user_profiles up 
    WHERE up.user_id = auth.uid() 
    AND up.user_type = 'student'
  )
);

-- Industries can view student profiles for matching
CREATE POLICY "Industries can view student profiles" 
ON public.user_profiles 
FOR SELECT 
USING (
  user_type = 'student' 
  AND EXISTS (
    SELECT 1 FROM user_profiles up 
    WHERE up.user_id = auth.uid() 
    AND up.user_type = 'industry'
  )
);

-- Add security definer function to clean up orphaned profiles (security measure)
CREATE OR REPLACE FUNCTION public.cleanup_orphaned_profiles()
RETURNS void AS $$
BEGIN
  -- Delete any profiles where the user_id doesn't exist in auth.users
  DELETE FROM public.user_profiles 
  WHERE user_id NOT IN (
    SELECT id FROM auth.users
  );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;