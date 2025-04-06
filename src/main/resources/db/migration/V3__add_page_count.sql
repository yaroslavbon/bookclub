-- Add page_count column to books table
ALTER TABLE books ADD COLUMN IF NOT EXISTS page_count INTEGER;