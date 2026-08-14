import React, { useEffect, useState } from 'react'
import { Select } from 'antd'
import { getCohorts } from '../api/cohort'
import type { Cohort } from '../types/cohort'

interface Props {
  value?: number | null
  onChange?: (value: number | null) => void
  /** 仅显示启用的届次（默认 true） */
  enabledOnly?: boolean
  allowClear?: boolean
  placeholder?: string
  style?: React.CSSProperties
}

const CohortSelect: React.FC<Props> = ({
  value,
  onChange,
  enabledOnly = true,
  allowClear = false,
  placeholder = '请选择届次',
  style,
}) => {
  const [cohorts, setCohorts] = useState<Cohort[]>([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    setLoading(true)
    getCohorts()
      .then((list) => setCohorts(enabledOnly ? list.filter((c) => c.enabled) : list))
      .catch(() => setCohorts([]))
      .finally(() => setLoading(false))
  }, [enabledOnly])

  return (
    <Select
      value={value ?? undefined}
      onChange={(v) => onChange?.(v ?? null)}
      loading={loading}
      allowClear={allowClear}
      placeholder={placeholder}
      style={style}
      options={cohorts.map((c) => ({ label: `${c.year}届`, value: c.id }))}
    />
  )
}

export default CohortSelect
