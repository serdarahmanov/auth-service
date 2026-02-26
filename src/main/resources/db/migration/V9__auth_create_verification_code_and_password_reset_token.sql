-- ============================
-- Verification Code Table
-- ============================

CREATE TABLE verification_code (
                                   id BIGSERIAL PRIMARY KEY,

                                   code VARCHAR(255) NOT NULL,
                                   is_email_sent BOOLEAN NOT NULL DEFAULT FALSE,
                                   used BOOLEAN NOT NULL DEFAULT FALSE,
                                   expires_at TIMESTAMP NOT NULL,

                                   user_id BIGINT NOT NULL,

                                   created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
                                   updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

                                   CONSTRAINT fk_verification_code_user
                                       FOREIGN KEY (user_id)
                                           REFERENCES auth_users(id)
                                           ON DELETE CASCADE
);

-- One-to-one: enforce uniqueness
CREATE UNIQUE INDEX ux_verification_code_user
    ON verification_code(user_id);

CREATE UNIQUE INDEX ux_verification_code_code
    ON verification_code(code);


-- ============================
-- Password Reset Token Table
-- ============================

CREATE TABLE password_reset_token (
                                      id BIGSERIAL PRIMARY KEY,

                                      code VARCHAR(255) NOT NULL,
                                      email_sent BOOLEAN NOT NULL DEFAULT FALSE,
                                      used BOOLEAN NOT NULL DEFAULT FALSE,
                                      expires_at TIMESTAMP,

                                      user_id BIGINT NOT NULL,

                                      created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
                                      updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

                                      CONSTRAINT fk_password_reset_token_user
                                          FOREIGN KEY (user_id)
                                              REFERENCES auth_users(id)
                                              ON DELETE CASCADE
);

-- Multiple tokens per user allowed
CREATE INDEX ix_password_reset_token_user
    ON password_reset_token(user_id);

CREATE UNIQUE INDEX ux_password_reset_token_code
    ON password_reset_token(code);
