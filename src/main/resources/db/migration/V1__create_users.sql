CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,

                       email VARCHAR(255) NOT NULL UNIQUE,
                       username VARCHAR(255) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL,

                       password_set BOOLEAN NOT NULL DEFAULT FALSE,

                       first_name VARCHAR(255) NOT NULL,
                       last_name VARCHAR(255) NOT NULL,

                       enabled BOOLEAN NOT NULL DEFAULT FALSE,

                       avatar_key VARCHAR(500),
                       bio VARCHAR(500),

                       created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                       updated_at TIMESTAMP
);