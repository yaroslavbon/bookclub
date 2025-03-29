-- Members table
CREATE TABLE IF NOT EXISTS members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

-- Books table
CREATE TABLE IF NOT EXISTS books (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    author VARCHAR(255) NOT NULL,
    is_fiction BOOLEAN DEFAULT FALSE,
    owner_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    comments TEXT,
    cover_image_path VARCHAR(255),
    completion_date DATE,
    file_paths JSON,
    FOREIGN KEY (owner_id) REFERENCES members(id)
);

-- Member queue table (for rotation order)
CREATE TABLE IF NOT EXISTS member_queue (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL UNIQUE,
    position INT NOT NULL,
    last_pick_date DATE,
    total_picks INT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (member_id) REFERENCES members(id)
);

-- Ratings table
CREATE TABLE IF NOT EXISTS ratings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    book_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    readability_rating INT,
    content_rating INT,
    comments TEXT,
    date_rated DATE NOT NULL,
    did_not_read BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (book_id) REFERENCES books(id),
    FOREIGN KEY (member_id) REFERENCES members(id),
    CONSTRAINT unique_book_member UNIQUE (book_id, member_id)
);

-- Users table (for authentication)
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL
);

-- Initial admin user (password: admin)
INSERT INTO users (username, password_hash, role)
VALUES ('admin', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'ADMIN');

-- Initial user (password: user)
INSERT INTO users (username, password_hash, role)
VALUES ('user', '$2a$10$gNpnGpIb3snViTjZ.a1EKezwKBGCdVmHZ6OxW/gGh6uqvs.t1y.se', 'USER');