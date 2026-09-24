import React from 'react'
import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  Cell,
} from 'recharts'
import type { RiskSummary } from '../../types'
import { useTheme } from '../../hooks/useTheme'

interface RiskDistributionChartProps {
  summary: RiskSummary
}

export const RiskDistributionChart: React.FC<RiskDistributionChartProps> = ({ summary }) => {
  const { theme } = useTheme()
  const isDark = theme === 'dark'

  const data = [
    { name: 'Low', count: summary.lowRiskTransactions, color: isDark ? '#34d399' : '#10b981' },
    { name: 'Medium', count: summary.mediumRiskTransactions, color: isDark ? '#fbbf24' : '#f59e0b' },
    { name: 'High', count: summary.highRiskTransactions, color: isDark ? '#fb923c' : '#f97316' },
    { name: 'Critical', count: summary.criticalRiskTransactions, color: isDark ? '#f87171' : '#ef4444' },
  ]

  const total = summary.totalTransactions || 1

  return (
    <div className="p-5 rounded-xl border border-zinc-200/80 dark:border-zinc-800/80 bg-white dark:bg-[#14161b] shadow-2xs">
      <div className="flex items-center justify-between mb-4">
        <div>
          <h3 className="text-sm font-semibold text-zinc-900 dark:text-zinc-100">
            Risk Level Distribution
          </h3>
          <p className="text-xs text-zinc-500 dark:text-zinc-400">
            Deterministic rule severity breakdown
          </p>
        </div>
        <span className="text-xs font-mono text-zinc-400 dark:text-zinc-500">
          {summary.totalFindings} total findings
        </span>
      </div>

      <div className="h-48 w-full">
        <ResponsiveContainer width="100%" height="100%">
          <BarChart data={data} layout="vertical" margin={{ top: 5, right: 20, left: 10, bottom: 5 }}>
            <XAxis
              type="number"
              tickLine={false}
              axisLine={false}
              tick={{ fontSize: 11, fill: isDark ? '#71717a' : '#a1a1aa' }}
            />
            <YAxis
              type="category"
              dataKey="name"
              tickLine={false}
              axisLine={false}
              tick={{ fontSize: 12, fill: isDark ? '#d4d4d8' : '#3f3f46' }}
            />
            <Tooltip
              cursor={{ fill: isDark ? 'rgba(255,255,255,0.03)' : 'rgba(0,0,0,0.03)' }}
              content={({ active, payload }) => {
                if (active && payload && payload.length) {
                  const item = payload[0].payload
                  const pct = ((item.count / total) * 100).toFixed(1)
                  return (
                    <div className="p-2.5 rounded-lg border border-zinc-200 dark:border-zinc-700 bg-white/95 dark:bg-zinc-800/95 backdrop-blur-sm shadow-md text-xs">
                      <p className="font-semibold text-zinc-900 dark:text-zinc-100">{item.name} Risk</p>
                      <p className="text-zinc-500 dark:text-zinc-400">
                        {item.count} transactions ({pct}%)
                      </p>
                    </div>
                  )
                }
                return null
              }}
            />
            <Bar dataKey="count" radius={[0, 4, 4, 0]} barSize={16}>
              {data.map((entry, index) => (
                <Cell key={`cell-${index}`} fill={entry.color} />
              ))}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      </div>

      {/* Mini Legend Row */}
      <div className="grid grid-cols-4 gap-2 pt-3 mt-2 border-t border-zinc-100 dark:border-zinc-800/60 text-center">
        {data.map((d) => (
          <div key={d.name} className="flex flex-col">
            <span className="text-[10px] uppercase font-medium text-zinc-400 dark:text-zinc-500">
              {d.name}
            </span>
            <span className="text-xs font-mono font-semibold text-zinc-800 dark:text-zinc-200">
              {d.count}
            </span>
          </div>
        ))}
      </div>
    </div>
  )
}
