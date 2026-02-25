CREATE TABLE oauth2_auth_code(
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(255) NOT NULL,
    username VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX ux_oauth2_auth_code
    ON oauth2_auth_code(code);

CREATE INDEX ix_oauth2_auth_code_expires_at
    ON oauth2_auth_code(expires_at);
