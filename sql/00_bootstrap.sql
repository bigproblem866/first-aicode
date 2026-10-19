-- =============================================================================
-- 00_bootstrap.sql —— ERP 开发环境数据库引导脚本
-- -----------------------------------------------------------------------------
-- 这是**唯一**需要用 root 权限执行一次的脚本。之后建表由 Flyway 在应用启动时
-- 自动完成（见 erp-server/erp-boot/src/main/resources/db/migration/）。
--
-- 执行方式（Git Bash，mysql.exe 不在 PATH 里，需写全路径）：
--   "/c/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe" -uroot -p < sql/00_bootstrap.sql
--
-- 幂等：可以重复执行。
-- =============================================================================

CREATE DATABASE IF NOT EXISTS `erp_dev`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

-- 应用专用账号（不用 root 跑应用）。
-- MySQL 8 的 'localhost' 与 '%' 是两条独立记录，两个都建，
-- 避免将来从 IDEA / Docker / 局域网连时踩 "Access denied"。
CREATE USER IF NOT EXISTS 'erp'@'localhost' IDENTIFIED BY 'Erp@123456';
CREATE USER IF NOT EXISTS 'erp'@'%'         IDENTIFIED BY 'Erp@123456';

GRANT ALL PRIVILEGES ON `erp_dev`.* TO 'erp'@'localhost';
GRANT ALL PRIVILEGES ON `erp_dev`.* TO 'erp'@'%';
FLUSH PRIVILEGES;

-- ---------------------------------------------------------------------------
-- 自检：执行完应看到 mysql 版本 / utf8mb4 / 时区
-- ---------------------------------------------------------------------------
SELECT VERSION() AS mysql_version,
       @@character_set_database AS charset_db,
       @@collation_database AS collation_db,
       @@global.time_zone AS global_tz,
       @@session.time_zone AS session_tz;
