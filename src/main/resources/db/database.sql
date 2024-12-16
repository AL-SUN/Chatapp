CREATE DATABASE IF NOT EXISTS chatapp;
USE chatapp;

CREATE TABLE IF NOT EXISTS users (
    username VARCHAR(50) PRIMARY KEY,
    password VARCHAR(300) NOT NULL
);

INSERT IGNORE INTO users (username, password) VALUES ('System', '1234'); -- default user

CREATE TABLE IF NOT EXISTS rooms (
    rid INTEGER PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,             
    -- type ENUM('public', 'private') NOT NULL DEFAULT 'public',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT IGNORE INTO rooms (name) VALUES ('default'); -- default room

CREATE TABLE IF NOT EXISTS messages (
    mid INTEGER PRIMARY KEY AUTO_INCREMENT,
    room_id INTEGER NOT NULL,
    sender  VARCHAR(50) NOT NULL,
    message TEXT NOT NULL,                
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, 
    FOREIGN KEY (room_id) REFERENCES rooms (rid),
    FOREIGN KEY (sender) REFERENCES users (username)
);

-- -- load latest messages
-- SELECT sender, message, sent_at
-- FROM messages
-- WHERE m.room_id = ?
-- ORDER BY m.sent_at DESC
-- LIMIT 20 OFFSET 0; -- 20 messages per page
--
-- -- search messages
-- SELECT * FROM messages
-- WHERE room_id = ? AND message LIKE '%???????%'
-- ORDER BY sent_at DESC;