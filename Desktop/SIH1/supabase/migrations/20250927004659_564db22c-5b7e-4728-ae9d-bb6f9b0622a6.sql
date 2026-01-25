-- Fix the overly permissive RLS policy on students table
-- Drop the existing policy that allows all authenticated users to view students
DROP POLICY IF EXISTS "Authenticated users can view students for migration" ON public.students;

-- Create restrictive policies for students table
-- Students can only view their own data
CREATE POLICY "Students can view their own data" 
ON public.students 
FOR SELECT 
USING (
  EXISTS (
    SELECT 1 FROM auth.users 
    WHERE auth.users.id = auth.uid() 
    AND auth.users.email = students.email
  )
);

-- Industries can view students only for matching purposes (but not email addresses)
-- This will be handled by the matching functions which use service role
-- No direct access for industries to student emails

-- Admins can view all student data (when admin role is implemented)
CREATE POLICY "Admins can view all students" 
ON public.students 
FOR SELECT 
USING (
  EXISTS (
    SELECT 1 FROM public.user_profiles 
    WHERE user_profiles.user_id = auth.uid() 
    AND user_profiles.user_type = 'admin'
  )
);

-- Update students table to prevent unauthorized access to sensitive data
-- Remove the permissive INSERT/UPDATE policies if they exist
DROP POLICY IF EXISTS "Students can insert their own data" ON public.students;
DROP POLICY IF EXISTS "Students can update their own data" ON public.students;

-- Only allow students to update their own profiles via the user_profiles table
-- The students table should be read-only for security