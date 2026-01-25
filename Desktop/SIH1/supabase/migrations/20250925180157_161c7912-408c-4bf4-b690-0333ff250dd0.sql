-- Drop the problematic policies that cause infinite recursion
DROP POLICY IF EXISTS "Students can view industry profiles" ON public.user_profiles;
DROP POLICY IF EXISTS "Industries can view student profiles" ON public.user_profiles;

-- Create security definer functions to avoid infinite recursion
CREATE OR REPLACE FUNCTION public.get_current_user_type()
RETURNS TEXT AS $$
  SELECT user_type FROM public.user_profiles WHERE user_id = auth.uid();
$$ LANGUAGE SQL SECURITY DEFINER STABLE SET search_path = public;

-- Create new policies using the security definer function
CREATE POLICY "Students can view industry profiles" 
ON public.user_profiles 
FOR SELECT 
USING (
  user_type = 'industry' 
  AND public.get_current_user_type() = 'student'
);

CREATE POLICY "Industries can view student profiles" 
ON public.user_profiles 
FOR SELECT 
USING (
  user_type = 'student' 
  AND public.get_current_user_type() = 'industry'
);

-- Update the function to have proper search path
CREATE OR REPLACE FUNCTION public.cleanup_orphaned_profiles()
RETURNS void AS $$
BEGIN
  DELETE FROM public.user_profiles 
  WHERE user_id NOT IN (
    SELECT id FROM auth.users
  );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;