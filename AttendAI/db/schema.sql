-- FaceAttend database schema
-- Works with SQLite as-is. For MySQL: change AUTOINCREMENT -> AUTO_INCREMENT
-- and INTEGER PRIMARY KEY -> INT PRIMARY KEY AUTO_INCREMENT.

CREATE TABLE IF NOT EXISTS users (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    user_code         TEXT NOT NULL UNIQUE,   -- roll no. / employee id
    name              TEXT NOT NULL,
    class_or_dept     TEXT,
    photo_sample_path TEXT,
    created_at        DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS attendance (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id    INTEGER NOT NULL,
    date       TEXT NOT NULL,   -- ISO format YYYY-MM-DD
    time       TEXT NOT NULL,   -- ISO format HH:MM:SS
    confidence REAL,
    status     TEXT NOT NULL,   -- PRESENT / UNKNOWN / LOW_CONFIDENCE
    FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE (user_id, date)      -- enforces "no duplicate entry for same day"
);

CREATE TABLE IF NOT EXISTS admins (
    id       INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL
);
