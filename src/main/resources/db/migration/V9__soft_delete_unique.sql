-- =====================================================
-- V9__soft_delete_unique.sql
-- 修复「软删除 + UNIQUE 约束」冲突
-- =====================================================
-- 问题：原 FULL UNIQUE 约束把已软删除行也纳入唯一性校验，而 Java 层
-- （@SQLRestriction("deleted_at IS NULL") / existsBy...DeletedAtIsNull...）
-- 只查未删除记录，两者不一致——把唯一字段改成某个「已软删除记录」曾用的值时，
-- Java 校验通过但数据库仍拒绝 → DataIntegrityViolationException 以 500 暴露。
--
-- 解决：改为「仅对未删除行」生效的部分唯一索引，与软删除语义保持一致，
-- 同时让已有的 Java 重复校验（只查未删除记录）与数据库约束完全对齐。
-- =====================================================

-- 1. members.student_no（学号）
ALTER TABLE members DROP CONSTRAINT members_student_no_key;
CREATE UNIQUE INDEX uq_members_student_no_live ON members(student_no) WHERE deleted_at IS NULL;

-- 2. users.username（登录账号）
ALTER TABLE users DROP CONSTRAINT users_username_key;
CREATE UNIQUE INDEX uq_users_username_live ON users(username) WHERE deleted_at IS NULL;

-- 3. cohorts.year（届次年份）
ALTER TABLE cohorts DROP CONSTRAINT cohorts_year_key;
CREATE UNIQUE INDEX uq_cohorts_year_live ON cohorts(year) WHERE deleted_at IS NULL;
