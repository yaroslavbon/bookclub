-- Add indices for common queries
CREATE INDEX IF NOT EXISTS idx_books_status ON books(status);
CREATE INDEX IF NOT EXISTS idx_books_owner_id ON books(owner_id);
CREATE INDEX IF NOT EXISTS idx_books_owner_status ON books(owner_id, status);
CREATE INDEX IF NOT EXISTS idx_books_completion_date ON books(completion_date);

CREATE INDEX IF NOT EXISTS idx_member_queue_position ON member_queue(position);
CREATE INDEX IF NOT EXISTS idx_member_queue_member_id ON member_queue(member_id);
CREATE INDEX IF NOT EXISTS idx_ratings_book_id ON ratings(book_id);
CREATE INDEX IF NOT EXISTS idx_ratings_member_id ON ratings(member_id);