-- Add new columns to matches table for detailed match information
ALTER TABLE public.matches
ADD COLUMN IF NOT EXISTS skill_match integer,
ADD COLUMN IF NOT EXISTS location_match boolean DEFAULT false,
ADD COLUMN IF NOT EXISTS rural_priority boolean DEFAULT false,
ADD COLUMN IF NOT EXISTS fresher_friendly boolean DEFAULT false;