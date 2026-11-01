-- The admin seeded by V2 has the well-known password "123".
-- Disable it unless the password has already been changed;
-- set ADMIN_PASSWORD to (re)create a working admin on startup.
update m_user
set active = false
where username = 'admin'
  and password = crypt('123', password);
