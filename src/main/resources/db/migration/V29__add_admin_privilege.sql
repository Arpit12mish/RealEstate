-- 1) add admin flag
ALTER TABLE users
ADD COLUMN IF NOT EXISTS is_admin boolean NOT NULL DEFAULT false;

-- 2) allow only ONE admin user in whole system
CREATE UNIQUE INDEX IF NOT EXISTS ux_users_single_admin
ON users ((is_admin))
WHERE is_admin = true;

-- 3) If no admin exists yet, mark exactly ONE user as admin (choose one row deterministically)
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM users WHERE is_admin = true) THEN
    UPDATE users
    SET role = 'CUSTOMER',
        is_admin = true
    WHERE id = (
      SELECT id
      FROM users
      WHERE phone_number IN ('+918877712345', '8877712345')
      ORDER BY (phone_number LIKE '+91%') DESC, id ASC   -- prefer +91, then smallest id
      LIMIT 1
    );
  END IF;
END $$;

-- 4) normalize any accidental ADMIN role
UPDATE users
SET role = 'CUSTOMER'
WHERE role = 'ADMIN';
