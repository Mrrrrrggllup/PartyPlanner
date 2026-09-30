-- Grant CSV export access to specific users.
-- Run once after deploying the backend (the can_export column is added automatically by SchemaUtils).
UPDATE users SET can_export = true
WHERE email IN ('antoine.zudas@gmail.com', 'nolwenngagnardeau@gmail.com');
