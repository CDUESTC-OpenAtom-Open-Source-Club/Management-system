import React, { useState } from 'react'
import { Empty } from 'antd'
import type { WeeklyTrend } from '../../types/dashboard'

interface Props {
  data: WeeklyTrend[]
}

const W = 600
const H = 200
const PAD_L = 28
const PAD_R = 24
const PAD_T = 20
const PAD_B = 30

function formatAvg(total: number): string {
  const avg = total / 7
  if (avg === 0) return '0'
  return avg < 1 ? avg.toFixed(2) : avg.toFixed(1)
}

const TrendChart: React.FC<Props> = ({ data }) => {
  const [active, setActive] = useState<number | null>(null)

  if (!data.length || data.every((d) => d.count === 0)) {
    return (
      <div className="chart-empty">
        <Empty
          image={Empty.PRESENTED_IMAGE_SIMPLE}
          description={
            <>
              <div className="chart-empty-title">近 7 周暂无积分登记</div>
              <div className="chart-empty-sub">产生积分登记后将在这里展示趋势。</div>
            </>
          }
        />
      </div>
    )
  }

  const total = data.reduce((s, d) => s + d.count, 0)
  const max = Math.max(...data.map((d) => d.count), 1)
  const plotW = W - PAD_L - PAD_R
  const plotH = H - PAD_T - PAD_B
  const xStep = data.length > 1 ? plotW / (data.length - 1) : plotW
  // 0.85 预留顶部空间，避免 max 值折线顶到顶部
  const points = data.map((d, i) => ({
    x: PAD_L + i * xStep,
    y: PAD_T + plotH * (1 - (d.count / max) * 0.85),
    count: d.count,
    week: d.week,
  }))

  const linePath = points.map((p, i) => `${i === 0 ? 'M' : 'L'} ${p.x} ${p.y}`).join(' ')
  const areaPath = `${linePath} L ${points[points.length - 1].x} ${PAD_T + plotH} L ${points[0].x} ${PAD_T + plotH} Z`

  const gridYs = [0, 1, 2, 3, 4].map((i) => PAD_T + (plotH / 4) * i)

  return (
    <div className="trend-chart-wrap">
      <div className="trend-summary">
        近7周共 {total} 条登记 · 周均 {formatAvg(total)} · 峰值 {max}
      </div>
      <svg className="trend-chart" viewBox={`0 0 ${W} ${H}`} preserveAspectRatio="none" role="img" aria-label="近 7 周积分登记趋势">
        <defs>
          <linearGradient id="trendArea" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#2f6bff" stopOpacity="0.14" />
            <stop offset="100%" stopColor="#2f6bff" stopOpacity="0" />
          </linearGradient>
        </defs>

        {gridYs.map((y) => (
          <line key={y} x1={PAD_L} y1={y} x2={W - PAD_R} y2={y} stroke="#eaecf0" strokeWidth="1" />
        ))}

        <path d={areaPath} fill="url(#trendArea)" />
        <path d={linePath} fill="none" stroke="#2f6bff" strokeWidth="2" strokeLinejoin="round" strokeLinecap="round" />

        {active !== null && (
          <line
            x1={points[active].x}
            y1={PAD_T}
            x2={points[active].x}
            y2={PAD_T + plotH}
            stroke="#d0d5dd"
            strokeWidth="1"
            strokeDasharray="3 3"
          />
        )}

        {points.map((p, i) => (
          <circle
            key={p.week}
            cx={p.x}
            cy={p.y}
            r={active === i ? 5 : 3}
            fill="#ffffff"
            stroke="#2f6bff"
            strokeWidth="2"
            onMouseEnter={() => setActive(i)}
            onMouseLeave={() => setActive(null)}
          />
        ))}

        {points.map((p) => (
          <text key={p.week} x={p.x} y={H - 8} textAnchor="middle" fontSize="11" fill="#98a2b3">
            {`W${p.week}`}
          </text>
        ))}

        {active !== null && (
          <g>
            <rect
              x={Math.min(Math.max(points[active].x - 44, PAD_L - 8), W - PAD_R - 88)}
              y={Math.max(points[active].y - 40, 2)}
              width="88"
              height="24"
              rx="7"
              fill="#101828"
            />
            <text
              x={Math.min(Math.max(points[active].x, PAD_L + 36), W - PAD_R - 44)}
              y={Math.max(points[active].y - 40, 2) + 16}
              textAnchor="middle"
              fontSize="12"
              fill="#ffffff"
            >
              {`第 ${points[active].week} 周 · ${points[active].count} 条`}
            </text>
          </g>
        )}
      </svg>
    </div>
  )
}

export default TrendChart
