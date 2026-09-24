import React, { useEffect, useState, useMemo } from 'react'
import {
  Search,
  Filter,
  RefreshCw,
  ChevronLeft,
  ChevronRight,
  SlidersHorizontal,
} from 'lucide-react'
import { riskApi } from '../api/risk'
import type { RiskTransactionResponse, RiskSeverity } from '../types'
import { TransactionTable } from '../components/transactions/TransactionTable'
import { TableSkeleton } from '../components/common/Skeleton'
import { ErrorState } from '../components/common/ErrorState'
import { EmptyState } from '../components/common/EmptyState'

export const Transactions: React.FC = () => {
  const [transactions, setTransactions] = useState<RiskTransactionResponse[]>([])
  const [totalElements, setTotalElements] = useState(0)
  const [totalPages, setTotalPages] = useState(1)
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(10)

  const [riskLevel, setRiskLevel] = useState<RiskSeverity | ''>('')
  const [minScore, setMinScore] = useState<number | ''>('')
  const [searchQuery, setSearchQuery] = useState('')

  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const loadTransactions = async () => {
    try {
      setLoading(true)
      setError(null)

      const params: {
        page: number
        size: number
        riskLevel?: RiskSeverity
        minScore?: number
      } = {
        page,
        size: pageSize,
      }

      if (riskLevel) params.riskLevel = riskLevel
      if (minScore !== '') params.minScore = Number(minScore)

      const response = await riskApi.getTransactions(params)
      setTransactions(response.content || [])
      setTotalElements(response.totalElements || 0)
      setTotalPages(response.totalPages || 1)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load transactions')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadTransactions()
  }, [page, pageSize, riskLevel, minScore])

  // Client-side text search filter for vendor / employee / category / transactionId
  const filteredTransactions = useMemo(() => {
    if (!searchQuery.trim()) return transactions
    const q = searchQuery.toLowerCase()
    return transactions.filter(
      (tx) =>
        tx.transactionId.toLowerCase().includes(q) ||
        tx.vendor.toLowerCase().includes(q) ||
        tx.employee.toLowerCase().includes(q) ||
        (tx.category && tx.category.toLowerCase().includes(q))
    )
  }, [transactions, searchQuery])

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold tracking-tight text-zinc-900 dark:text-zinc-100">
            Transaction Explorer
          </h1>
          <p className="text-xs sm:text-sm text-zinc-500 dark:text-zinc-400 mt-0.5">
            Audit-grade visibility across deterministic risk indicators
          </p>
        </div>

        <button
          onClick={loadTransactions}
          disabled={loading}
          className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium border border-zinc-200 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-zinc-700 dark:text-zinc-300 hover:bg-zinc-50 dark:hover:bg-zinc-700 transition-colors self-start sm:self-auto"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
          Refresh
        </button>
      </div>

      {/* Filter Bar */}
      <div className="p-3.5 rounded-xl border border-zinc-200/80 dark:border-zinc-800/80 bg-white dark:bg-[#14161b] shadow-2xs flex flex-col md:flex-row items-stretch md:items-center justify-between gap-3">
        {/* Search input */}
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-zinc-400" />
          <input
            type="text"
            placeholder="Search by ID, Vendor, Employee, Category..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-9 pr-3 py-1.5 text-xs rounded-lg border border-zinc-200 dark:border-zinc-700 bg-zinc-50 dark:bg-zinc-900 text-zinc-900 dark:text-zinc-100 placeholder-zinc-400 focus:outline-none focus:ring-1 focus:ring-zinc-400 dark:focus:ring-zinc-500"
          />
        </div>

        {/* Filter controls */}
        <div className="flex flex-wrap items-center gap-2.5">
          {/* Risk Level Filter */}
          <div className="flex items-center gap-1.5">
            <Filter className="w-3.5 h-3.5 text-zinc-400" />
            <select
              value={riskLevel}
              onChange={(e) => {
                setRiskLevel(e.target.value as RiskSeverity | '')
                setPage(0)
              }}
              className="text-xs py-1.5 px-2.5 rounded-lg border border-zinc-200 dark:border-zinc-700 bg-zinc-50 dark:bg-zinc-900 text-zinc-800 dark:text-zinc-200 focus:outline-none"
            >
              <option value="">All Risk Levels</option>
              <option value="LOW">Low</option>
              <option value="MEDIUM">Medium</option>
              <option value="HIGH">High</option>
              <option value="CRITICAL">Critical</option>
            </select>
          </div>

          {/* Min Score Filter */}
          <div className="flex items-center gap-1.5">
            <SlidersHorizontal className="w-3.5 h-3.5 text-zinc-400" />
            <select
              value={minScore}
              onChange={(e) => {
                setMinScore(e.target.value === '' ? '' : Number(e.target.value))
                setPage(0)
              }}
              className="text-xs py-1.5 px-2.5 rounded-lg border border-zinc-200 dark:border-zinc-700 bg-zinc-50 dark:bg-zinc-900 text-zinc-800 dark:text-zinc-200 focus:outline-none"
            >
              <option value="">Min Score: Any</option>
              <option value="30">Min Score: 30+</option>
              <option value="50">Min Score: 50+</option>
              <option value="60">Min Score: 60+</option>
              <option value="80">Min Score: 80+</option>
            </select>
          </div>

          {(riskLevel !== '' || minScore !== '' || searchQuery !== '') && (
            <button
              onClick={() => {
                setRiskLevel('')
                setMinScore('')
                setSearchQuery('')
                setPage(0)
              }}
              className="text-xs text-zinc-500 hover:text-zinc-900 dark:hover:text-zinc-100 underline underline-offset-2 px-1"
            >
              Reset
            </button>
          )}
        </div>
      </div>

      {/* Main Table Content */}
      <div className="rounded-xl border border-zinc-200/80 dark:border-zinc-800/80 bg-white dark:bg-[#14161b] shadow-2xs overflow-hidden">
        {loading ? (
          <div className="p-6">
            <TableSkeleton rows={pageSize} cols={7} />
          </div>
        ) : error ? (
          <div className="p-8">
            <ErrorState title="Error Loading Transactions" message={error} onRetry={loadTransactions} />
          </div>
        ) : filteredTransactions.length > 0 ? (
          <>
            <TransactionTable transactions={filteredTransactions} />

            {/* Pagination Controls */}
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 p-4 border-t border-zinc-200/80 dark:border-zinc-800/80 text-xs text-zinc-500 dark:text-zinc-400 bg-zinc-50/50 dark:bg-zinc-900/30">
              <div className="flex items-center gap-2">
                <span>
                  Showing{' '}
                  <span className="font-semibold text-zinc-900 dark:text-zinc-100">
                    {totalElements > 0 ? page * pageSize + 1 : 0}
                  </span>{' '}
                  to{' '}
                  <span className="font-semibold text-zinc-900 dark:text-zinc-100">
                    {Math.min((page + 1) * pageSize, totalElements)}
                  </span>{' '}
                  of{' '}
                  <span className="font-semibold text-zinc-900 dark:text-zinc-100">
                    {totalElements}
                  </span>{' '}
                  transactions
                </span>

                <select
                  value={pageSize}
                  onChange={(e) => {
                    setPageSize(Number(e.target.value))
                    setPage(0)
                  }}
                  className="ml-2 py-0.5 px-1.5 rounded border border-zinc-200 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-zinc-700 dark:text-zinc-300"
                >
                  <option value={5}>5 per page</option>
                  <option value={10}>10 per page</option>
                  <option value={20}>20 per page</option>
                  <option value={50}>50 per page</option>
                </select>
              </div>

              <div className="flex items-center gap-2 self-end sm:self-auto">
                <button
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  disabled={page === 0}
                  className="inline-flex items-center gap-1 px-2.5 py-1 rounded-md border border-zinc-200 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-zinc-700 dark:text-zinc-300 disabled:opacity-40 disabled:cursor-not-allowed hover:bg-zinc-50 dark:hover:bg-zinc-700/50 transition-colors"
                >
                  <ChevronLeft className="w-3.5 h-3.5" />
                  Previous
                </button>
                <span className="px-2 font-mono">
                  {page + 1} / {totalPages || 1}
                </span>
                <button
                  onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                  disabled={page >= totalPages - 1}
                  className="inline-flex items-center gap-1 px-2.5 py-1 rounded-md border border-zinc-200 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-zinc-700 dark:text-zinc-300 disabled:opacity-40 disabled:cursor-not-allowed hover:bg-zinc-50 dark:hover:bg-zinc-700/50 transition-colors"
                >
                  Next
                  <ChevronRight className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>
          </>
        ) : (
          <div className="p-8">
            <EmptyState
              title="No Matching Transactions"
              description="No transaction records match the current filter criteria."
              actionLabel="Clear Filters"
              onAction={() => {
                setRiskLevel('')
                setMinScore('')
                setSearchQuery('')
                setPage(0)
              }}
            />
          </div>
        )}
      </div>
    </div>
  )
}
