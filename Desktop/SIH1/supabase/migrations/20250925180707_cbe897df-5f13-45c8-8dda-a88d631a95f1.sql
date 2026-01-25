-- Enable pgvector extension for AI embeddings
CREATE EXTENSION IF NOT EXISTS vector;

-- Create internships table
CREATE TABLE IF NOT EXISTS public.internships (
  id UUID NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
  industry_id UUID NOT NULL,
  title TEXT NOT NULL,
  role TEXT NOT NULL,
  description TEXT,
  required_skills JSONB NOT NULL DEFAULT '[]'::jsonb,
  location TEXT NOT NULL,
  sector TEXT NOT NULL,
  capacity INTEGER NOT NULL DEFAULT 1,
  duration_months INTEGER DEFAULT 3,
  stipend_amount INTEGER,
  is_active BOOLEAN NOT NULL DEFAULT true,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- Enable RLS on internships table
ALTER TABLE public.internships ENABLE ROW LEVEL SECURITY;

-- RLS policies for internships
CREATE POLICY "Industries can manage their own internships" 
ON public.internships 
FOR ALL 
USING (
  industry_id = auth.uid() 
  AND EXISTS (
    SELECT 1 FROM user_profiles up 
    WHERE up.user_id = auth.uid() 
    AND up.user_type = 'industry'
  )
);

CREATE POLICY "Students can view active internships" 
ON public.internships 
FOR SELECT 
USING (
  is_active = true 
  AND EXISTS (
    SELECT 1 FROM user_profiles up 
    WHERE up.user_id = auth.uid() 
    AND up.user_type = 'student'
  )
);

CREATE POLICY "Admins can view all internships" 
ON public.internships 
FOR SELECT 
USING (
  EXISTS (
    SELECT 1 FROM user_profiles up 
    WHERE up.user_id = auth.uid() 
    AND up.user_type = 'admin'
  )
);

-- Update matches table to include application status
ALTER TABLE public.matches ADD COLUMN IF NOT EXISTS application_status TEXT DEFAULT 'pending';
ALTER TABLE public.matches ADD COLUMN IF NOT EXISTS applied_at TIMESTAMP WITH TIME ZONE DEFAULT now();
ALTER TABLE public.matches ADD COLUMN IF NOT EXISTS shortlisted_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE public.matches ADD COLUMN IF NOT EXISTS rejected_at TIMESTAMP WITH TIME ZONE;

-- Add internship_id foreign key to matches if it doesn't exist
ALTER TABLE public.matches ADD COLUMN IF NOT EXISTS internship_id UUID;

-- Add trigger for updated_at on internships
CREATE TRIGGER update_internships_updated_at
BEFORE UPDATE ON public.internships
FOR EACH ROW
EXECUTE FUNCTION public.update_updated_at_column();

-- Add additional fields to user_profiles for better matching
ALTER TABLE public.user_profiles ADD COLUMN IF NOT EXISTS experience_level TEXT;
ALTER TABLE public.user_profiles ADD COLUMN IF NOT EXISTS portfolio_url TEXT;
ALTER TABLE public.user_profiles ADD COLUMN IF NOT EXISTS linkedin_url TEXT;
ALTER TABLE public.user_profiles ADD COLUMN IF NOT EXISTS preferences JSONB DEFAULT '{}'::jsonb;

-- Create skill embeddings table for AI matching
CREATE TABLE IF NOT EXISTS public.skill_embeddings (
  id UUID NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
  skill_name TEXT NOT NULL UNIQUE,
  embedding VECTOR(1536), -- OpenAI embedding dimension
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- Enable RLS on skill_embeddings
ALTER TABLE public.skill_embeddings ENABLE ROW LEVEL SECURITY;

-- Allow all authenticated users to read skill embeddings
CREATE POLICY "Authenticated users can read skill embeddings" 
ON public.skill_embeddings 
FOR SELECT 
USING (auth.uid() IS NOT NULL);

-- Create notifications table
CREATE TABLE IF NOT EXISTS public.notifications (
  id UUID NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
  user_id UUID NOT NULL,
  title TEXT NOT NULL,
  message TEXT NOT NULL,
  type TEXT NOT NULL DEFAULT 'info',
  is_read BOOLEAN NOT NULL DEFAULT false,
  related_id UUID, -- Can reference match_id, internship_id, etc.
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  FOREIGN KEY (user_id) REFERENCES auth.users(id) ON DELETE CASCADE
);

-- Enable RLS on notifications
ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;

-- Users can only see their own notifications
CREATE POLICY "Users can view their own notifications" 
ON public.notifications 
FOR SELECT 
USING (auth.uid() = user_id);

CREATE POLICY "Users can update their own notifications" 
ON public.notifications 
FOR UPDATE 
USING (auth.uid() = user_id);

-- System can create notifications for users
CREATE POLICY "System can create notifications" 
ON public.notifications 
FOR INSERT 
WITH CHECK (true);