import React, { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { SearchCode, ArrowRight } from 'lucide-react'
import { riskApi } from '../api/risk'
import type { RiskTransactionResponse } from '../types'
import { RiskBadge } from '../components/common/RiskBadge'
import { RiskScoreGauge } from '../components/common/RiskScoreGauge'
import { TableSkeleton } from '../components/common/Skeleton'
import { ErrorState } from '../components/common/ErrorState'
import { EmptyState } from '../components/common/EmptyState'
import { formatCurrency } from '../lib/utils'

export const InvestigationsPage: React.FC = () => {
  const [highRiskTransactions, setHighRiskTransactions] = useState<RiskTransactionResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const loadInvestigations = async () => {
    try {
      setLoading(true)
      setError(null)
      const res = await riskApi.getTransactions({ minScore: 50, size: 50 })
      setHighRiskTransactions(res.content || [])
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load high risk investigations')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadInvestigations()
  }, [])

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-xl sm:text-2xl font-bold tracking-tight text-zinc-900 dark:text-zinc-100 flex items-center gap-2.5">
          <SearchCode className="w-6 h-6 text-zinc-600 dark:text-zinc-400" />
          Active Investigations
        </h1>
        <p className="text-xs sm:text-sm text-zinc-500 dark:text-zinc-400 mt-0.5">
          Prioritized queue of transactions scoring 50+ requiring compliance investigation
        </p>
      </div>

      <div className="rounded-xl border border-zinc-200/80 dark:border-zinc-800/80 bg-white dark:bg-[#14161b] shadow-2xs overflow-hidden">
        {loading ? (
          <div className="p-6">
            <TableSkeleton rows={6} cols={6} />
          </div>
        ) : error ? (
          <div className="p-8">
            <ErrorState title="Error Loading Queue" message={error} onRetry={loadInvestigations} />
          </div>
        ) : highRiskTransactions.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-zinc-50/75 dark:bg-zinc-900/50 border-b border-zinc-200/80 dark:border-zinc-800/80 text-zinc-500 dark:text-zinc-400">
                <tr>
                  <th className="py-3 px-4 font-medium">Transaction ID</th>
                  <th className="py-3 px-4 font-medium">Vendor</th>
                  <th className="py-3 px-4 font-medium">Employee</th>
                  <th className="py-3 px-4 font-medium text-right">Amount</th>
                  <th className="py-3 px-4 font-medium text-center">Score</th>
                  <th className="py-3 px-4 font-medium text-center">Severity</th>
                  <th className="py-3 px-4 font-medium text-center">Findings</th>
                  <th className="py-3 px-4 font-medium text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-100 dark:divide-zinc-800/60">
                {highRiskTransactions.map((tx) => (
                  <tr
                    key={tx.transactionId}
                    className="hover:bg-zinc-50/80 dark:hover:bg-zinc-800/40 transition-colors"
                  >
                    <td className="py-3.5 px-4 font-mono font-medium text-zinc-900 dark:text-zinc-100">
                      <Link
                        to={`/transactions/${tx.transactionId}`}
                        className="hover:underline underline-offset-2"
                      >
                        {tx.transactionId}
                      </Link>
                    </td>
                    <td className="py-3.5 px-4 text-zinc-800 dark:text-zinc-200 font-medium">
                      {tx.vendor}
                    </td>
                    <td className="py-3.5 px-4 text-zinc-600 dark:text-zinc-400">{tx.employee}</td>
                    <td className="py-3.5 px-4 text-right font-mono font-medium text-zinc-900 dark:text-zinc-100">
                      {formatCurrency(tx.amount)}
                    </td>
                    <td className="py-3.5 px-4 text-center">
                      <RiskScoreGauge score={tx.riskScore} size="sm" showLabel={false} />
                    </td>
                    <td className="py-3.5 px-4 text-center">
                      <RiskBadge severity={tx.riskLevel} />
                    </td>
                    <td className="py-3.5 px-4 text-center font-mono text-zinc-600 dark:text-zinc-400">
                      {tx.findings?.length || 0}
                    </td>
                    <td className="py-3.5 px-4 text-right">
                      <Link
                        to={`/transactions/${tx.transactionId}`}
                        className="inline-flex items-center gap-1.5 px-3 py-1 rounded text-xs font-semibold bg-zinc-900 dark:bg-zinc-100 text-white dark:text-zinc-900 hover:bg-zinc-800 dark:hover:bg-zinc-200 transition-colors shadow-2xs"
                      >
                        Investigate
                        <ArrowRight className="w-3.5 h-3.5" />
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
              title="Queue Clear"
              description="There are currently no transactions flagged above the 50-point risk investigation threshold."
              actionLabel="View All Transactions"
              actionLink="/transactions"
            />
          </div>
        )}
      </div>
    </div>
  )
}
