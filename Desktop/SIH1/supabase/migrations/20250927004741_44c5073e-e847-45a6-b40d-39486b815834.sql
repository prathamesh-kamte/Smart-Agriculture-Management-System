-- Fix the critical security issue: Remove overly permissive RLS policy on students table
-- This policy currently allows ANY authenticated user to read ALL student data including emails

-- Drop the dangerous policy that allows all authenticated users to view students
DROP POLICY IF EXISTS "Authenticated users can view students for migration" ON public.students;

-- Note: The "Students can view their own data" and "Admins can view all students" policies 
-- already exist and provide proper access control. The legacy "students" table should 
-- eventually be migrated to use user_profiles exclusively for better security.