-- Upgrading from Version 1: goals you created before accounts existed have no owner (user_id is NULL),
-- so nobody can see them. After you have registered an account in the app, give those goals to it.
--
-- 1. Change the email below to the one you registered with.
-- 2. Run:  mysql -u root -p goalstack < database\claim_v1_goals.sql

UPDATE goals
SET user_id = (SELECT id FROM users WHERE email = 'you@example.com')
WHERE user_id IS NULL;

-- Check the result:
SELECT COUNT(*) AS goals_without_owner FROM goals WHERE user_id IS NULL;
