import React, { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  ReceiptText,
  ShieldAlert,
  CircleDollarSign,
  Layers,
  ArrowUpRight,
} from 'lucide-react'
import { riskApi } from '../api/risk'
import type { RiskSummary, RiskTransactionResponse } from '../types'
import { MetricCard } from '../components/dashboard/MetricCard'
import { RiskDistributionChart } from '../components/dashboard/RiskDistributionChart'
import { RecentRiskTable } from '../components/dashboard/RecentRiskTable'
import { MetricCardSkeleton, TableSkeleton } from '../components/common/Skeleton'
import { ErrorState } from '../components/common/ErrorState'
import { EmptyState } from '../components/common/EmptyState'
import { formatCurrency } from '../lib/utils'

export const Dashboard: React.FC = () => {
  const [summary, setSummary] = useState<RiskSummary | null>(null)
  const [recentHighRisk, setRecentHighRisk] = useState<RiskTransactionResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const loadData = async () => {
    try {
      setLoading(true)
      setError(null)

      const [summaryData, highRiskData] = await Promise.all([
        riskApi.getSummary(),
        riskApi.getTransactions({ size: 5, minScore: 30 }),
      ])

      setSummary(summaryData)
      setRecentHighRisk(highRiskData.content || [])
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load dashboard metrics')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadData()
  }, [])

  const getGreeting = () => {
    const hour = new Date().getHours()
    if (hour < 12) return 'Good morning'
    if (hour < 17) return 'Good afternoon'
    return 'Good evening'
  }

  if (error) {
    return <ErrorState title="Dashboard Unavailable" message={error} onRetry={loadData} />
  }

  return (
    <div className="space-y-6 sm:space-y-8">
      {/* Header section */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold tracking-tight text-zinc-900 dark:text-zinc-100">
            {getGreeting()}
          </h1>
          <p className="text-xs sm:text-sm text-zinc-500 dark:text-zinc-400 mt-0.5">
            Here's what's happening with your financial risk data.
          </p>
        </div>

        <div className="flex items-center gap-2.5">
          <Link
            to="/transactions"
            className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-lg text-xs font-medium bg-zinc-900 dark:bg-zinc-100 text-white dark:text-zinc-900 hover:bg-zinc-800 dark:hover:bg-zinc-200 transition-colors shadow-2xs"
          >
            Explore Transactions
            <ArrowUpRight className="w-3.5 h-3.5" />
          </Link>
        </div>
      </div>

      {/* Metrics Row */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-3.5 sm:gap-4">
        {loading ? (
          Array.from({ length: 5 }).map((_, i) => <MetricCardSkeleton key={i} />)
        ) : summary ? (
          <>
            <MetricCard
              title="Total Transactions"
              value={summary.totalTransactions.toLocaleString()}
              subtitle="Audited in database"
              icon={<ReceiptText className="w-4 h-4" />}
            />

            <MetricCard
              title="Analyzed Data"
              value={summary.totalTransactions.toLocaleString()}
              subtitle={`${summary.totalFindings} flags generated`}
              icon={<Layers className="w-4 h-4" />}
            />

            <MetricCard
              title="High Risk"
              value={summary.highRiskTransactions}
              subtitle="Score between 60–79"
              highlightColor="orange"
              icon={<ShieldAlert className="w-4 h-4" />}
            />

            <MetricCard
              title="Critical Risk"
              value={summary.criticalRiskTransactions}
              subtitle="Score 80 and above"
              highlightColor="rose"
              icon={<ShieldAlert className="w-4 h-4" />}
            />

            <MetricCard
              title="Total Audit Value"
              value={formatCurrency(summary.totalAmount)}
              subtitle="Cumulative volume"
              icon={<CircleDollarSign className="w-4 h-4" />}
            />
          </>
        ) : null}
      </div>

      {/* Charts & Breakdown */}
      {loading ? (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="lg:col-span-1 p-6 rounded-xl border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-900">
            <TableSkeleton rows={3} cols={2} />
          </div>
          <div className="lg:col-span-2 p-6 rounded-xl border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-900">
            <TableSkeleton rows={4} cols={4} />
          </div>
        </div>
      ) : summary && summary.totalTransactions > 0 ? (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="lg:col-span-1">
            <RiskDistributionChart summary={summary} />
          </div>

          <div className="lg:col-span-2">
            {recentHighRisk.length > 0 ? (
              <RecentRiskTable transactions={recentHighRisk} />
            ) : (
              <EmptyState
                title="No High Risk Transactions"
                description="All evaluated transactions currently fall below the high-risk score threshold."
                actionLabel="View All Transactions"
                actionLink="/transactions"
              />
            )}
          </div>
        </div>
      ) : (
        <EmptyState
          title="No Transaction Records Available"
          description="Import transaction datasets to run deterministic risk engine rules and view analysis."
          actionLabel="Go to Import"
          actionLink="/import"
        />
      )}
    </div>
  )
}
