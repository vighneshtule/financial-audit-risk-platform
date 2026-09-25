import React, { useEffect, useState, useMemo } from 'react'
import {
  BarChart2,
  Building2,
  Users,
  ShieldAlert,
  Tag,
  TrendingUp,
  AlertTriangle,
  CheckCircle2,
  Circle,
  RefreshCw,
  ArrowUpRight,
} from 'lucide-react'
import { Link } from 'react-router-dom'
import { riskApi } from '../api/risk'
import type { RiskIntelligenceSummary } from '../types'
import { MetricCardSkeleton } from '../components/common/Skeleton'
import { ErrorState } from '../components/common/ErrorState'
import { EmptyState } from '../components/common/EmptyState'
import { formatCurrency } from '../lib/utils'

function riskColor(level: string): string {
  switch (level) {
    case 'CRITICAL': return 'text-red-500 dark:text-red-400'
    case 'HIGH':     return 'text-orange-500 dark:text-orange-400'
    case 'MEDIUM':   return 'text-amber-500 dark:text-amber-400'
    case 'LOW':      return 'text-emerald-500 dark:text-emerald-400'
    default:         return 'text-zinc-400'
  }
}

function riskBg(level: string): string {
  switch (level) {
    case 'CRITICAL': return 'bg-red-500/10 border-red-500/20 text-red-500 dark:text-red-400'
    case 'HIGH':     return 'bg-orange-500/10 border-orange-500/20 text-orange-500 dark:text-orange-400'
    case 'MEDIUM':   return 'bg-amber-500/10 border-amber-500/20 text-amber-500 dark:text-amber-400'
    case 'LOW':      return 'bg-emerald-500/10 border-emerald-500/20 text-emerald-500 dark:text-emerald-400'
    default:         return 'bg-zinc-500/10 border-zinc-500/20 text-zinc-400'
  }
}

function riskBarColor(level: string): string {
  switch (level) {
    case 'CRITICAL': return 'bg-red-500'
    case 'HIGH':     return 'bg-orange-500'
    case 'MEDIUM':   return 'bg-amber-500'
    case 'LOW':      return 'bg-emerald-500'
    default:         return 'bg-zinc-400'
  }
}

function ruleLabel(riskType: string): string {
  const map: Record<string, string> = {
    HIGH_AMOUNT:               'High Amount',
    UNUSUAL_TRANSACTION_TIME:  'Unusual Time',
    DUPLICATE_TRANSACTION:     'Duplicate',
    ROUND_AMOUNT:              'Round Amount',
    TRANSACTION_VELOCITY:      'High Velocity',
    VENDOR_CONCENTRATION:      'Vendor Concentration',
  }
  return map[riskType] ?? riskType.replace(/_/g, ' ')
}

function scoreToLevel(score: number): string {
  if (score >= 80) return 'CRITICAL'
  if (score >= 60) return 'HIGH'
  if (score >= 30) return 'MEDIUM'
  return 'LOW'
}

const SectionHeader: React.FC<{ icon: React.ReactNode; title: string; subtitle?: string }> = ({
  icon, title, subtitle,
}) => (
  <div className="flex items-start gap-3 mb-5">
    <div className="w-8 h-8 rounded-lg bg-zinc-100 dark:bg-zinc-800 flex items-center justify-center text-zinc-600 dark:text-zinc-300 flex-shrink-0 mt-0.5">
      {icon}
    </div>
    <div>
      <h2 className="text-sm font-semibold text-zinc-900 dark:text-zinc-100 tracking-tight">{title}</h2>
      {subtitle && <p className="text-xs text-zinc-500 dark:text-zinc-400 mt-0.5">{subtitle}</p>}
    </div>
  </div>
)

const Card: React.FC<{ children: React.ReactNode; className?: string }> = ({ children, className = '' }) => (
  <div className={`rounded-xl border border-zinc-200/80 dark:border-zinc-800/70 bg-white dark:bg-zinc-900/60 p-5 ${className}`}>
    {children}
  </div>
)

interface BarRowProps {
  label: string
  value: number
  maxValue: number
  barColor: string
  meta?: React.ReactNode
}

const BarRow: React.FC<BarRowProps> = ({ label, value, maxValue, barColor, meta }) => {
  const pct = maxValue > 0 ? Math.max(2, Math.round((value / maxValue) * 100)) : 0
  return (
    <div className="flex items-center gap-3 py-2 border-b border-zinc-100 dark:border-zinc-800/60 last:border-0">
      <div className="flex-1 min-w-0">
        <div className="flex items-center justify-between mb-1.5">
          <span className="text-xs font-medium text-zinc-800 dark:text-zinc-200 truncate max-w-[60%]">{label}</span>
          <span className="text-xs font-semibold text-zinc-700 dark:text-zinc-300 tabular-nums">{value}</span>
        </div>
        <div className="h-1.5 w-full rounded-full bg-zinc-100 dark:bg-zinc-800 overflow-hidden">
          <div className={`h-full rounded-full transition-all duration-700 ease-out ${barColor}`} style={{ width: `${pct}%` }} />
        </div>
      </div>
      {meta && <div className="flex-shrink-0">{meta}</div>}
    </div>
  )
}

export const RiskIntelligencePage: React.FC = () => {
  const [data, setData] = useState<RiskIntelligenceSummary | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [refreshing, setRefreshing] = useState(false)

  const loadData = async (isRefresh = false) => {
    try {
      if (isRefresh) setRefreshing(true)
      else { setLoading(true); setError(null) }
      const summary = await riskApi.getIntelligenceSummary()
      setData(summary)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load risk intelligence')
    } finally {
      setLoading(false)
      setRefreshing(false)
    }
  }

  useEffect(() => { loadData() }, [])

  const maxVendorFindings = useMemo(() => Math.max(1, ...(data?.vendors.map(v => v.findingCount) ?? [1])), [data])
  const maxEmpFindings    = useMemo(() => Math.max(1, ...(data?.employees.map(e => e.findingCount) ?? [1])), [data])
  const maxRuleFindings   = useMemo(() => Math.max(1, ...(data?.riskTypes.map(r => r.findingCount) ?? [1])), [data])
  const maxCatFindings    = useMemo(() => Math.max(1, ...(data?.categories.map(c => c.findingCount) ?? [1])), [data])
  const totalSeverity     = useMemo(() => data?.severityDistribution.reduce((s, r) => s + r.count, 0) ?? 0, [data])

  if (error) return <ErrorState title="Risk Intelligence Unavailable" message={error} onRetry={() => loadData()} />

  const hasData = data && data.overview.analyzedTransactions > 0

  return (
    <div className="space-y-6 sm:space-y-8">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold tracking-tight text-zinc-900 dark:text-zinc-100">Risk Intelligence</h1>
          <p className="text-xs sm:text-sm text-zinc-500 dark:text-zinc-400 mt-0.5">Dataset-level risk analytics — vendors, employees, rules, categories.</p>
        </div>
        <button
          id="ri-refresh-btn"
          onClick={() => loadData(true)}
          disabled={loading || refreshing}
          className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-lg text-xs font-medium bg-zinc-900 dark:bg-zinc-100 text-white dark:text-zinc-900 hover:bg-zinc-700 dark:hover:bg-zinc-200 transition-colors disabled:opacity-50 shadow-xs"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${refreshing ? 'animate-spin' : ''}`} />
          Refresh
        </button>
      </div>

      {/* KPI strip */}
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3">
        {loading
          ? Array.from({ length: 6 }).map((_, i) => <MetricCardSkeleton key={i} />)
          : data
          ? [
              { label: 'Transactions',   value: data.overview.totalTransactions.toLocaleString(),    icon: <BarChart2 className="w-4 h-4" />,    highlight: false },
              { label: 'Analyzed',       value: data.overview.analyzedTransactions.toLocaleString(), icon: <CheckCircle2 className="w-4 h-4" />, highlight: false },
              { label: 'Total Findings', value: data.overview.totalFindings.toLocaleString(),        icon: <AlertTriangle className="w-4 h-4" />,highlight: false },
              { label: 'Audit Value',    value: formatCurrency(data.overview.totalAuditValue),       icon: <TrendingUp className="w-4 h-4" />,   highlight: false },
              { label: 'Peak Score',     value: String(data.overview.highestRiskScore),              icon: <ShieldAlert className="w-4 h-4" />,  highlight: false },
              { label: 'Critical',       value: data.overview.criticalRisk.toLocaleString(),         icon: <Circle className="w-4 h-4" />,       highlight: true  },
            ].map((kpi) => (
              <div key={kpi.label} className={`rounded-xl border p-4 flex flex-col gap-2 shadow-xs ${kpi.highlight ? 'border-red-200/60 dark:border-red-900/30 bg-red-50/60 dark:bg-red-950/20' : 'border-zinc-200/80 dark:border-zinc-800/70 bg-white dark:bg-zinc-900/60'}`}>
                <div className={`w-7 h-7 rounded-md flex items-center justify-center ${kpi.highlight ? 'bg-red-100 dark:bg-red-900/40 text-red-500' : 'bg-zinc-100 dark:bg-zinc-800 text-zinc-500 dark:text-zinc-400'}`}>
                  {kpi.icon}
                </div>
                <p className="text-[10px] font-medium text-zinc-500 dark:text-zinc-400 uppercase tracking-wider leading-none">{kpi.label}</p>
                <p className={`text-lg font-bold tracking-tight leading-none ${kpi.highlight ? 'text-red-600 dark:text-red-400' : 'text-zinc-900 dark:text-zinc-100'}`}>{kpi.value}</p>
              </div>
            ))
          : null}
      </div>

      {!loading && !hasData && (
        <EmptyState
          title="No Risk Analysis Data"
          description="Run a risk analysis on your transactions to populate Risk Intelligence."
          actionLabel="Go to Overview"
          actionLink="/"
        />
      )}

      {!loading && hasData && (
        <>
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
            {/* Severity distribution */}
            <Card>
              <SectionHeader icon={<ShieldAlert className="w-4 h-4" />} title="Severity Distribution" subtitle="Risk level breakdown across analyzed transactions" />
              <div>
                {data!.severityDistribution.map((row) => {
                  const pct = totalSeverity > 0 ? ((row.count / totalSeverity) * 100).toFixed(1) : '0.0'
                  return (
                    <div key={row.severity} className="flex items-center gap-3 py-2.5 border-b border-zinc-100 dark:border-zinc-800/60 last:border-0">
                      <span className={`text-xs font-semibold w-16 uppercase tracking-wider ${riskColor(row.severity)}`}>{row.severity}</span>
                      <div className="flex-1 h-2 rounded-full bg-zinc-100 dark:bg-zinc-800 overflow-hidden">
                        <div className={`h-full rounded-full transition-all duration-700 ease-out ${riskBarColor(row.severity)}`} style={{ width: `${totalSeverity > 0 ? Math.max(2, (row.count / totalSeverity) * 100) : 0}%` }} />
                      </div>
                      <span className="text-xs tabular-nums font-semibold text-zinc-700 dark:text-zinc-300 w-6 text-right">{row.count}</span>
                      <span className={`text-[10px] font-medium px-1.5 py-0.5 rounded border ${riskBg(row.severity)} w-12 text-center`}>{pct}%</span>
                    </div>
                  )
                })}
              </div>
            </Card>

            {/* Rule frequency */}
            <Card>
              <SectionHeader icon={<BarChart2 className="w-4 h-4" />} title="Rule Trigger Frequency" subtitle="Which risk rules fired most across the dataset" />
              {data!.riskTypes.length === 0
                ? <p className="text-xs text-zinc-500 dark:text-zinc-400">No rule findings recorded yet.</p>
                : data!.riskTypes.map((rt) => (
                    <BarRow key={rt.riskType} label={ruleLabel(rt.riskType)} value={rt.findingCount} maxValue={maxRuleFindings} barColor="bg-violet-500"
                      meta={<span className="text-[10px] text-zinc-500 dark:text-zinc-400 tabular-nums">+{rt.totalScoreContribution} pts</span>} />
                  ))}
            </Card>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
            {/* Vendor risk */}
            <Card>
              <SectionHeader icon={<Building2 className="w-4 h-4" />} title="Vendor Risk Exposure" subtitle="Ranked by flagged transactions and total findings" />
              {data!.vendors.length === 0
                ? <p className="text-xs text-zinc-500 dark:text-zinc-400">No vendor data available.</p>
                : data!.vendors.slice(0, 8).map((v) => (
                    <BarRow key={v.vendor} label={v.vendor} value={v.findingCount} maxValue={maxVendorFindings} barColor="bg-blue-500"
                      meta={
                        <div className="flex flex-col items-end gap-0.5">
                          <span className={`text-[10px] font-semibold px-1.5 py-0.5 rounded border ${riskBg(scoreToLevel(v.highestRiskScore))}`}>{v.highestRiskScore}</span>
                          <span className="text-[10px] text-zinc-500 dark:text-zinc-400 tabular-nums">{v.flaggedTransactionCount}/{v.transactionCount}</span>
                        </div>
                      } />
                  ))}
            </Card>

            {/* Employee risk */}
            <Card>
              <SectionHeader icon={<Users className="w-4 h-4" />} title="Employee Risk Exposure" subtitle="Ranked by flagged transactions and total findings" />
              {data!.employees.length === 0
                ? <p className="text-xs text-zinc-500 dark:text-zinc-400">No employee data available.</p>
                : data!.employees.slice(0, 8).map((e) => (
                    <BarRow key={e.employee} label={e.employee} value={e.findingCount} maxValue={maxEmpFindings} barColor="bg-teal-500"
                      meta={
                        <div className="flex flex-col items-end gap-0.5">
                          <span className={`text-[10px] font-semibold px-1.5 py-0.5 rounded border ${riskBg(scoreToLevel(e.highestRiskScore))}`}>{e.highestRiskScore}</span>
                          <span className="text-[10px] text-zinc-500 dark:text-zinc-400 tabular-nums">{e.flaggedTransactionCount}/{e.transactionCount}</span>
                        </div>
                      } />
                  ))}
            </Card>
          </div>

          {/* Category breakdown */}
          <Card>
            <SectionHeader icon={<Tag className="w-4 h-4" />} title="Category Risk Breakdown" subtitle="Spend categories ordered by flagged transactions and findings" />
            {data!.categories.length === 0
              ? <p className="text-xs text-zinc-500 dark:text-zinc-400">No category data available.</p>
              : (
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-x-8">
                  {data!.categories.map((cat) => (
                    <div key={cat.category} className="flex items-center gap-3 py-2.5 border-b border-zinc-100 dark:border-zinc-800/60 last:border-0">
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center justify-between mb-1.5">
                          <span className="text-xs font-medium text-zinc-800 dark:text-zinc-200 truncate max-w-[55%]">{cat.category}</span>
                          <div className="flex items-center gap-2 text-[10px] text-zinc-500 dark:text-zinc-400 tabular-nums">
                            <span>{cat.flaggedTransactionCount}/{cat.transactionCount}</span>
                            <span>·</span>
                            <span>{formatCurrency(cat.totalAmount)}</span>
                          </div>
                        </div>
                        <div className="h-1.5 w-full rounded-full bg-zinc-100 dark:bg-zinc-800 overflow-hidden">
                          <div className="h-full rounded-full bg-indigo-500 transition-all duration-700 ease-out" style={{ width: `${maxCatFindings > 0 ? Math.max(2, (cat.findingCount / maxCatFindings) * 100) : 0}%` }} />
                        </div>
                      </div>
                      <span className="text-xs font-semibold text-zinc-700 dark:text-zinc-300 tabular-nums w-6 text-right">{cat.findingCount}</span>
                    </div>
                  ))}
                </div>
              )}
          </Card>
        </>
      )}
    </div>
  )
}
