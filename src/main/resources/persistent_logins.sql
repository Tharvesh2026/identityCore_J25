-- ═══════════════════════════════════════════════════════════════════
-- Run this ONCE against your Postgres (Supabase) database.
-- This is Spring Security's standard schema for JdbcTokenRepositoryImpl —
-- do not rename columns, Spring Security's SQL is hardcoded against these.
-- ═══════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS persistent_logins (
    username  VARCHAR(64)  NOT NULL,
    series    VARCHAR(64)  PRIMARY KEY,
    token     VARCHAR(64)  NOT NULL,
    last_used TIMESTAMP    NOT NULL
);

-- Optional but recommended: speeds up the periodic cleanup query if you
-- later add a scheduled job to purge stale remember-me tokens.
CREATE INDEX IF NOT EXISTS idx_persistent_logins_username
    ON persistent_logins (username);
