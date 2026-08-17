/**
 * 积分项目类型统一定义。
 * 顺序即展示顺序，新增/编辑下拉框、筛选、Tag/文案统一复用，禁止各页面散落硬编码。
 */
export const POINT_ITEM_TYPES = [
  '活动',
  '比赛',
  '开源学习',
  '社区贡献',
  '演讲或主持',
  '其他',
] as const

export type PointItemType = (typeof POINT_ITEM_TYPES)[number]
