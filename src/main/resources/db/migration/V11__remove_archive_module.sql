-- =====================================================
-- V11__remove_archive_module.sql
-- 彻底删除「活动资料归档」模块
-- 活动资料统一改由「开源网盘（AList）」管理，系统不再维护 archive 表。
-- =====================================================

-- 先删关系表（archive_cohorts 引用 archive_links 与 cohorts），再删主表。
-- 删除表会一并删除其上的索引（idx_archive_cohorts_* / idx_archive_links_year）
-- 与外键约束（archive_cohorts.archive_id → archive_links.id）。
DROP TABLE IF EXISTS archive_cohorts;
DROP TABLE IF EXISTS archive_links;
