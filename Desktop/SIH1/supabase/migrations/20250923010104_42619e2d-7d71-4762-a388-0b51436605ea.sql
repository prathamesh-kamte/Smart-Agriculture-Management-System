-- Fix RLS policies for students table to allow public registration
DROP POLICY IF EXISTS "Enable insert for authenticated users only" ON public.students;

-- Create a policy that allows anyone to insert students (for registration)
CREATE POLICY "Enable insert for public registration" 
ON public.students 
FOR INSERT 
WITH CHECK (true);

-- Create a policy that allows users to view all students (for matching purposes)
CREATE POLICY "Enable select for all users" 
ON public.students 
FOR SELECT 
USING (true);

-- Create a policy that allows users to update their own records
CREATE POLICY "Enable update for users based on email" 
ON public.students 
FOR UPDATE 
USING (email = current_setting('app.current_user_email', true));

-- Enable RLS on industries table and create similar policies
ALTER TABLE public.industries ENABLE ROW LEVEL SECURITY;

-- Allow public registration for industries
CREATE POLICY "Enable insert for public registration" 
ON public.industries 
FOR INSERT 
WITH CHECK (true);

-- Allow viewing all industries
CREATE POLICY "Enable select for all users" 
ON public.industries 
FOR SELECT 
USING (true);

-- Allow industries to update their own records
CREATE POLICY "Enable update for users based on email" 
ON public.industries 
FOR UPDATE 
USING (email = current_setting('app.current_user_email', true));

-- Enable RLS on matches table and create policies
ALTER TABLE public.matches ENABLE ROW LEVEL SECURITY;

-- Allow creating matches
CREATE POLICY "Enable insert for matches" 
ON public.matches 
FOR INSERT 
WITH CHECK (true);

-- Allow viewing all matches
CREATE POLICY "Enable select for matches" 
ON public.matches 
FOR SELECT 
USING (true);

-- Allow updating matches
CREATE POLICY "Enable update for matches" 
ON public.matches 
FOR UPDATE 
USING (true);