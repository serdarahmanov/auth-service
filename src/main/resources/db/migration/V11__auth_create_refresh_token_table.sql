CREATE TABLE refresh_token (
    id BIGSERIAL PRIMARY KEY,

    token_hash VARCHAR(128) NOT NULL,
    family_id VARCHAR(64) NOT NULL,
    replaced_by_id BIGINT,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP,
    last_used_at TIMESTAMP,
    user_agent VARCHAR(512),
    ip_address VARCHAR(64),

    user_id BIGINT NOT NULL,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_refresh_token_user
        FOREIGN KEY (user_id)
            REFERENCES auth_users(id)
            ON DELETE CASCADE
);

CREATE UNIQUE INDEX ux_refresh_token_hash
    ON refresh_token(token_hash);

CREATE INDEX ix_refresh_token_user
    ON refresh_token(user_id);

CREATE INDEX ix_refresh_token_family
    ON refresh_token(family_id);
