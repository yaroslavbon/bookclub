-- Add authentication fields to members table
ALTER TABLE members ADD COLUMN password_hash VARCHAR(255) NOT NULL DEFAULT '$2a$10$dL4az.3o9RMK/CHfS3t5L.I1drt7NiNvGlSL3towKZ3WXin9r6Y0G'; -- Default password 'password'
ALTER TABLE members ADD COLUMN role VARCHAR(50) NOT NULL DEFAULT 'USER';

-- Create book completion records table with composite primary key
CREATE TABLE book_completion_records (
    book_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    completion_date DATE NOT NULL,
    PRIMARY KEY (book_id, member_id),
    FOREIGN KEY (book_id) REFERENCES books(id),
    FOREIGN KEY (member_id) REFERENCES members(id)
);

-- Mark first member as admin
UPDATE members SET role = 'ADMIN' WHERE id = 1;