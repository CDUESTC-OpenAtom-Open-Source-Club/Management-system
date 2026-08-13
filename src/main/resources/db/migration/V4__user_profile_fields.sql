ALTER TABLE users
ADD COLUMN IF NOT EXISTS profile_completed BOOLEAN DEFAULT FALSE;

ALTER TABLE users
ADD COLUMN IF NOT EXISTS initial_password_changed BOOLEAN DEFAULT FALSE;

UPDATE users
SET profile_completed = TRUE,
    initial_password_changed = TRUE
WHERE username = 'lintao';
