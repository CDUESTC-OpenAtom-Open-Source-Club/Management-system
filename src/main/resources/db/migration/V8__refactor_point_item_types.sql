-- =====================================================
-- V8__refactor_point_item_types.sql
-- 积分项目类型重构：旧类型收口为新 6 类
--   会议 → 演讲或主持
--   任务 → 社区贡献
--   活动 / 其他 保持不变
-- =====================================================

UPDATE point_items SET item_type = '演讲或主持' WHERE item_type = '会议';
UPDATE point_items SET item_type = '社区贡献' WHERE item_type = '任务';
