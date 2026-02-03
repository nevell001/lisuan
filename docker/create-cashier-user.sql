CREATE USER IF NOT EXISTS 'cashier'@'%' IDENTIFIED WITH mysql_native_password BY 'YourStrongPassword123!';
GRANT ALL PRIVILEGES ON cashier_system.* TO 'cashier'@'%';
FLUSH PRIVILEGES;
SELECT 'Cashier user created successfully!' AS status;
SELECT user, host, plugin FROM mysql.user WHERE user='cashier';
