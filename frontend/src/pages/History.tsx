import React, { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  History as HistoryIcon,
  Search,
  ChevronRight,
  RefreshCw,
} from 'lucide-react'
import { riskApi } from '../api/risk'
import { RiskBadge } from '../components/common/RiskBadge'
import { RiskScoreGauge } from '../components/common/RiskScoreGauge'
import { TableSkeleton } from '../components/common/Skeleton'
import { ErrorState } from '../components/common/ErrorState'
import { EmptyState } from '../components/common/EmptyState'
import { formatDate } from '../lib/utils'

interface HistoryEntry {
  transactionId: string
  vendor?: string
  employee?: string
  runId: number
  analyzedAt: string
  riskScore: number
  riskLevel: string
  findingsCount: number
}

export const HistoryPage: React.FC = () => {
  const [historyList, setHistoryList] = useState<HistoryEntry[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [searchQuery, setSearchQuery] = useState('')

  const loadAllHistory = async () => {
    try {
      setLoading(true)
      setError(null)

      // Get transaction list first
      const txPage = await riskApi.getTransactions({ size: 50 })
      const transactions = txPage.content || []

      // For each transaction, query risk history in parallel
      const historyPromises = transactions.map(async (tx) => {
        try {
          const res = await riskApi.getTransactionHistory(tx.transactionId)
          return (res.analysisRuns || []).map((run) => ({
            transactionId: tx.transactionId,
            vendor: tx.vendor,
            employee: tx.employee,
            runId: run.analysisRunId,
            analyzedAt: run.analyzedAt,
            riskScore: run.riskScore,
            riskLevel: run.riskLevel,
            findingsCount: run.findings?.length || 0,
          }))
        } catch {
          return []
        }
      })

      const results = await Promise.all(historyPromises)
      const flattened = results.flat()

      // Sort latest first
      flattened.sort((a, b) => new Date(b.analyzedAt).getTime() - new Date(a.analyzedAt).getTime())
      setHistoryList(flattened)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to fetch audit history')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadAllHistory()
  }, [])

  const filtered = historyList.filter(
    (item) =>
      item.transactionId.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (item.vendor && item.vendor.toLowerCase().includes(searchQuery.toLowerCase())) ||
      (item.employee && item.employee.toLowerCase().includes(searchQuery.toLowerCase()))
  )

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold tracking-tight text-zinc-900 dark:text-zinc-100 flex items-center gap-2.5">
            <HistoryIcon className="w-6 h-6 text-zinc-600 dark:text-zinc-400" />
            Analysis Audit History
          </h1>
          <p className="text-xs sm:text-sm text-zinc-500 dark:text-zinc-400 mt-0.5">
            Chronological log of persisted deterministic risk evaluation snapshots
          </p>
        </div>

        <button
          onClick={loadAllHistory}
          disabled={loading}
          className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium border border-zinc-200 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-zinc-700 dark:text-zinc-300 hover:bg-zinc-50 dark:hover:bg-zinc-700 transition-colors self-start sm:self-auto"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
          Refresh
        </button>
      </div>

      {/* Filter */}
      <div className="p-3.5 rounded-xl border border-zinc-200/80 dark:border-zinc-800/80 bg-white dark:bg-[#14161b] shadow-2xs">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-zinc-400" />
          <input
            type="text"
            placeholder="Search history by Transaction ID, Vendor, Employee..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-9 pr-3 py-1.5 text-xs rounded-lg border border-zinc-200 dark:border-zinc-700 bg-zinc-50 dark:bg-zinc-900 text-zinc-900 dark:text-zinc-100 placeholder-zinc-400 focus:outline-none focus:ring-1 focus:ring-zinc-400 dark:focus:ring-zinc-500"
          />
        </div>
      </div>

      {/* Main Table */}
      <div className="rounded-xl border border-zinc-200/80 dark:border-zinc-800/80 bg-white dark:bg-[#14161b] shadow-2xs overflow-hidden">
        {loading ? (
          <div className="p-6">
            <TableSkeleton rows={8} cols={6} />
          </div>
        ) : error ? (
          <div className="p-8">
            <ErrorState title="History Load Error" message={error} onRetry={loadAllHistory} />
          </div>
        ) : filtered.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-zinc-50/75 dark:bg-zinc-900/50 border-b border-zinc-200/80 dark:border-zinc-800/80 text-zinc-500 dark:text-zinc-400">
                <tr>
                  <th className="py-3 px-4 font-medium">Run ID</th>
                  <th className="py-3 px-4 font-medium">Transaction ID</th>
                  <th className="py-3 px-4 font-medium">Vendor / Employee</th>
                  <th className="py-3 px-4 font-medium text-center">Risk Score</th>
                  <th className="py-3 px-4 font-medium text-center">Severity</th>
                  <th className="py-3 px-4 font-medium text-center">Findings</th>
                  <th className="py-3 px-4 font-medium text-right">Analyzed At</th>
                  <th className="py-3 px-4 font-medium text-right"></th>
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-100 dark:divide-zinc-800/60">
                {filtered.map((item) => (
                  <tr
                    key={`${item.transactionId}-${item.runId}`}
                    className="hover:bg-zinc-50/80 dark:hover:bg-zinc-800/40 transition-colors group cursor-pointer"
                  >
                    <td className="py-3.5 px-4 font-mono text-zinc-500 dark:text-zinc-400 font-medium">
                      #{item.runId}
                    </td>
                    <td className="py-3.5 px-4 font-mono font-medium text-zinc-900 dark:text-zinc-100">
                      <Link
                        to={`/transactions/${item.transactionId}`}
                        className="hover:underline underline-offset-2"
                      >
                        {item.transactionId}
                      </Link>
                    </td>
                    <td className="py-3.5 px-4 text-zinc-700 dark:text-zinc-300">
                      <div className="truncate max-w-[180px]">
                        <span className="font-medium text-zinc-900 dark:text-zinc-100">
                          {item.vendor || '—'}
                        </span>
                        <span className="text-[10px] text-zinc-400 block">{item.employee}</span>
                      </div>
                    </td>
                    <td className="py-3.5 px-4 text-center">
                      <RiskScoreGauge score={item.riskScore} size="sm" showLabel={false} />
                    </td>
                    <td className="py-3.5 px-4 text-center">
                      <RiskBadge severity={item.riskLevel} />
                    </td>
                    <td className="py-3.5 px-4 text-center font-mono text-zinc-700 dark:text-zinc-300">
                      {item.findingsCount}
                    </td>
                    <td className="py-3.5 px-4 text-right font-mono text-zinc-500 dark:text-zinc-400">
                      {formatDate(item.analyzedAt)}
                    </td>
                    <td className="py-3.5 px-4 text-right">
                      <Link
                        to={`/transactions/${item.transactionId}`}
                        className="p-1 rounded-md inline-flex items-center text-zinc-400 hover:text-zinc-900 dark:hover:text-zinc-100 hover:bg-zinc-100 dark:hover:bg-zinc-800 transition-colors"
                      >
                        <ChevronRight className="w-4 h-4" />
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="p-8">
            <EmptyState
              title="No Analysis History Found"
              description="Historical analysis runs are generated when audit transactions are evaluated and persisted."
              actionLabel="Explore Transactions"
              actionLink="/transactions"
            />
          </div>
        )}
      </div>
    </div>
  )
}
