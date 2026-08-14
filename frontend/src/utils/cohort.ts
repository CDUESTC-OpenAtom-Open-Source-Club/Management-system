/** 届次年份统一显示：2026 → "2026届"；null/undefined → "未分届" */
export function cohortLabel(year: number | null | undefined): string {
  return year == null ? '未分届' : `${year}届`
}
