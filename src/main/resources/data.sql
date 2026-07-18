-- ============================================================
-- i.Core Seed Data — H2 Compatible
-- Runs after JPA creates schema (defer-datasource-initialization=true)
-- ============================================================

-- ── Roles ────────────────────────────────────────────
INSERT INTO roles (role_name, status) VALUES ('USER',      'ACTIVE');
INSERT INTO roles (role_name, status) VALUES ('ADMIN',     'ACTIVE');
INSERT INTO roles (role_name, status) VALUES ('SYS_ADMIN', 'ACTIVE');
INSERT INTO roles (role_name, status) VALUES ('INSPECTOR', 'ACTIVE');

-- ── Permissions ──────────────────────────────────────
INSERT INTO permissions (permission_key, description) VALUES ('USER_READ',             'View users');
INSERT INTO permissions (permission_key, description) VALUES ('USER_CREATE',           'Create new users');
INSERT INTO permissions (permission_key, description) VALUES ('USER_UPDATE',           'Update user details');
INSERT INTO permissions (permission_key, description) VALUES ('USER_DELETE',           'Delete users');
INSERT INTO permissions (permission_key, description) VALUES ('USER_PASSWORD_RESET',   'Reset user password');
INSERT INTO permissions (permission_key, description) VALUES ('ROLE_READ',             'View roles');
INSERT INTO permissions (permission_key, description) VALUES ('ROLE_CREATE',           'Create custom roles');
INSERT INTO permissions (permission_key, description) VALUES ('ROLE_UPDATE',           'Manage roles');
INSERT INTO permissions (permission_key, description) VALUES ('ROLE_PERMISSION_MANAGE','Manage permissions assigned to roles');
INSERT INTO permissions (permission_key, description) VALUES ('PROFILE_READ',          'View profile');
INSERT INTO permissions (permission_key, description) VALUES ('PROFILE_UPDATE',        'Update profile');
INSERT INTO permissions (permission_key, description) VALUES ('LOG_VIEW',              'View application logs');
INSERT INTO permissions (permission_key, description) VALUES ('SETTINGS_UPDATE',       'Update application settings');

-- ── Role → Permissions ───────────────────────────────

-- USER role: only PROFILE_READ + PROFILE_UPDATE
INSERT INTO role_permissions (role_id, permission_id)
  SELECT r.id, p.id FROM roles r, permissions p
  WHERE r.role_name = 'USER'
    AND p.permission_key IN ('PROFILE_READ','PROFILE_UPDATE','LOG_VIEW');

-- INSPECTOR role: USER_READ + PROFILE_READ
INSERT INTO role_permissions (role_id, permission_id)
  SELECT r.id, p.id FROM roles r, permissions p
  WHERE r.role_name = 'INSPECTOR'
    AND p.permission_key IN ('USER_READ','PROFILE_READ','PROFILE_UPDATE','LOG_VIEW');

-- ADMIN role: everything except ROLE_PERMISSION_MANAGE + ROLE_CREATE
INSERT INTO role_permissions (role_id, permission_id)
  SELECT r.id, p.id FROM roles r, permissions p
  WHERE r.role_name = 'ADMIN'
    AND p.permission_key IN (
      'USER_READ','USER_CREATE','USER_UPDATE','USER_DELETE','USER_PASSWORD_RESET',
      'ROLE_READ','ROLE_UPDATE',
      'PROFILE_READ','PROFILE_UPDATE',
      'LOG_VIEW','SETTINGS_UPDATE'
    );

-- SYS_ADMIN role: all permissions
INSERT INTO role_permissions (role_id, permission_id)
  SELECT r.id, p.id FROM roles r, permissions p
  WHERE r.role_name = 'SYS_ADMIN';

-- ── Default SYS_ADMIN user ────────────────────────────
-- Password: Admin@123
INSERT INTO users (name, mail_id, username, password, role_id, status, provider, verified, created_at)
  SELECT 'System Admin', 'sysadmin@icore.dev', 'sysadmin',
         '$2a$12$gZ/CNeFCdfBe7bbAXdN2gujE3d46k7D7AcjPuyO9H47kwetWyTWTu',
         r.id, 'ACTIVE', 'LOCAL', true, CURRENT_TIMESTAMP
  FROM roles r WHERE r.role_name = 'SYS_ADMIN';

INSERT INTO users (name, mail_id, username, password, role_id, status, provider, verified, created_at)
  SELECT 'Admin', 'admin@icore.dev', 'admin',
         '$2a$12$gZ/CNeFCdfBe7bbAXdN2gujE3d46k7D7AcjPuyO9H47kwetWyTWTu',
         r.id, 'ACTIVE', 'LOCAL', true, CURRENT_TIMESTAMP
  FROM roles r WHERE r.role_name = 'ADMIN';

INSERT INTO users (name, mail_id, username, password, role_id, status, provider, verified, created_at)
  SELECT 'User', 'user@icore.dev', 'User',
         '$2y$12$eVW59sNFHpTpO8.xnM4DCejsVGNs6fFSbKt22RPAZp14Qyq99A83m',
         r.id, 'ACTIVE', 'LOCAL', true, CURRENT_TIMESTAMP
  FROM roles r WHERE r.role_name = 'USER';


CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE
    );