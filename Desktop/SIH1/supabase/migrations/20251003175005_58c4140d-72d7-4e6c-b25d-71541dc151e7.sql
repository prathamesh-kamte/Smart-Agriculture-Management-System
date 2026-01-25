-- Add match_reasons column to matches table
ALTER TABLE public.matches 
ADD COLUMN IF NOT EXISTS match_reasons jsonb DEFAULT '[]'::jsonb;

-- Add comment to explain the column
COMMENT ON COLUMN public.matches.match_reasons IS 'Array of reasons explaining why this match was made';