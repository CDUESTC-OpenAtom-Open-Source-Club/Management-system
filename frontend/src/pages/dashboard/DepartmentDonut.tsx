import React, { useMemo } from 'react'
import { Empty } from 'antd'
import type { DepartmentStat } from '../../types/dashboard'

const COLORS = ['#175CD3', '#2F6BFF', '#528BFF', '#84ADFF', '#B2CCFF', '#D1E0FF']

interface Segment {
  label: string
  value: number
  pct: number
  color: string
  dash: string
  offset: number
}

interface Props {
  data: DepartmentStat[]
}

const SIZE = 168
const STROKE = 28
const R = (SIZE - STROKE) / 2
const C = 2 * Math.PI * R

const DepartmentDonut: React.FC<Props> = ({ data }) => {
  const { total, segments, isSingle } = useMemo(() => {
    const total = data.reduce((s, d) => s + d.value, 0)
    if (total <= 0) return { total: 0, segments: [] as Segment[], isSingle: false }

    const sorted = [...data].sort((a, b) => b.value - a.value)
    const top = sorted.slice(0, 5)
    const restValue = sorted.slice(5).reduce((s, d) => s + d.value, 0)
    const display = top.map((d) => ({ label: d.label, value: d.value }))
    if (restValue > 0) display.push({ label: '其他', value: restValue })

    let acc = 0
    const segments = display.map((d, i) => {
      const len = (d.value / total) * C
      const seg = {
        label: d.label,
        value: d.value,
        pct: Math.round((d.value / total) * 1000) / 10,
        color: COLORS[i % COLORS.length],
        dash: `${len} ${C - len}`,
        offset: -acc,
      }
      acc += len
      return seg
    })
    return { total, segments, isSingle: display.length === 1 }
  }, [data])

  if (total === 0) {
    return (
      <div className="chart-empty">
        <Empty
          image={Empty.PRESENTED_IMAGE_SIMPLE}
          description={
            <>
              <div className="chart-empty-title">暂无部门成员数据</div>
              <div className="chart-empty-sub">录入成员后将在这里展示部门构成。</div>
            </>
          }
        />
      </div>
    )
  }

  const donut = (
    <div className="donut-chart">
      <svg viewBox={`0 0 ${SIZE} ${SIZE}`} width={SIZE} height={SIZE} role="img" aria-label="部门成员构成">
        <circle cx={SIZE / 2} cy={SIZE / 2} r={R} fill="none" stroke="#f2f4f7" strokeWidth={STROKE} />
        {segments.map((s) => (
          <circle
            key={s.label}
            cx={SIZE / 2}
            cy={SIZE / 2}
            r={R}
            fill="none"
            stroke={s.color}
            strokeWidth={STROKE}
            strokeDasharray={s.dash}
            strokeDashoffset={s.offset}
            transform={`rotate(-90 ${SIZE / 2} ${SIZE / 2})`}
          />
        ))}
      </svg>
      <div className="donut-center">
        <div className="donut-center-num">{total}</div>
        <div className="donut-center-label">总人数</div>
      </div>
    </div>
  )

  if (isSingle) {
    const s = segments[0]
    return (
      <div className="donut-wrap donut-wrap--single">
        {donut}
        <div className="donut-single">
          <div className="donut-single-label">{s.label}</div>
          <div className="donut-single-value">{s.value} 人</div>
          <div className="donut-single-pct">占成员总数 {s.pct}%</div>
        </div>
      </div>
    )
  }

  return (
    <div className="donut-wrap">
      {donut}
      <div className="donut-legend">
        {segments.map((s) => (
          <div className="donut-legend-row" key={s.label}>
            <span className="donut-dot" style={{ background: s.color }} />
            <span className="donut-legend-label">{s.label}</span>
            <span className="donut-legend-value">{s.value}</span>
            <span className="donut-legend-pct">{s.pct}%</span>
          </div>
        ))}
      </div>
    </div>
  )
}

export default DepartmentDonut
