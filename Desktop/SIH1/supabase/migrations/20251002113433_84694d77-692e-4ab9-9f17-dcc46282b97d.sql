-- Create feedback table for students and companies
CREATE TABLE IF NOT EXISTS public.feedback (
  id UUID NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
  match_id UUID NOT NULL REFERENCES public.matches(id) ON DELETE CASCADE,
  from_user_id UUID NOT NULL,
  from_user_type TEXT NOT NULL CHECK (from_user_type IN ('student', 'industry')),
  rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
  comment TEXT,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- Enable RLS
ALTER TABLE public.feedback ENABLE ROW LEVEL SECURITY;

-- Users can view their own feedback
CREATE POLICY "Users can view feedback they gave"
ON public.feedback
FOR SELECT
USING (auth.uid() = from_user_id);

-- Users can insert their own feedback
CREATE POLICY "Users can insert their own feedback"
ON public.feedback
FOR INSERT
WITH CHECK (auth.uid() = from_user_id);

-- Add index for performance
CREATE INDEX idx_feedback_match_id ON public.feedback(match_id);
CREATE INDEX idx_feedback_from_user_id ON public.feedback(from_user_id);