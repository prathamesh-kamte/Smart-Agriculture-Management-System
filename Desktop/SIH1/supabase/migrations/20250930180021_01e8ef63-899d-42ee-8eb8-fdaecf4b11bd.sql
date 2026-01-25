-- Add new columns to students table for rural/urban status and background
ALTER TABLE public.students
ADD COLUMN IF NOT EXISTS rural_status text,
ADD COLUMN IF NOT EXISTS background text;

-- Add new columns to user_profiles table as well (since that's the main profile table)
ALTER TABLE public.user_profiles
ADD COLUMN IF NOT EXISTS rural_status text,
ADD COLUMN IF NOT EXISTS background text,
ADD COLUMN IF NOT EXISTS is_fresher boolean DEFAULT false;

-- Add check constraints for valid values
ALTER TABLE public.students
ADD CONSTRAINT rural_status_check CHECK (rural_status IN ('Rural', 'Urban') OR rural_status IS NULL);

ALTER TABLE public.students
ADD CONSTRAINT background_check CHECK (background IN ('General', 'OBC/SC/ST/Other Backward Background') OR background IS NULL);

ALTER TABLE public.user_profiles
ADD CONSTRAINT up_rural_status_check CHECK (rural_status IN ('Rural', 'Urban') OR rural_status IS NULL);

ALTER TABLE public.user_profiles
ADD CONSTRAINT up_background_check CHECK (background IN ('General', 'OBC/SC/ST/Other Backward Background') OR background IS NULL);