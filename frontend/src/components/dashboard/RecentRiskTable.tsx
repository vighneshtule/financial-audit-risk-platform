import React from 'react'
import { Link } from 'react-router-dom'
import { ArrowRight, User, Building } from 'lucide-react'
import type { RiskTransactionResponse } from '../../types'
import { RiskBadge } from '../common/RiskBadge'
import { RiskScoreGauge } from '../common/RiskScoreGauge'
import { formatCurrency } from '../../lib/utils'

interface RecentRiskTableProps {
  transactions: RiskTransactionResponse[]
}

export const RecentRiskTable: React.FC<RecentRiskTableProps> = ({ transactions }) => {
  return (
    <div className="rounded-xl border border-zinc-200/80 dark:border-zinc-800/80 bg-white dark:bg-[#14161b] shadow-2xs overflow-hidden">
      <div className="flex items-center justify-between p-4 sm:p-5 border-b border-zinc-200/80 dark:border-zinc-800/80">
        <div>
          <h3 className="text-sm font-semibold text-zinc-900 dark:text-zinc-100">
            High-Risk Transactions
          </h3>
          <p className="text-xs text-zinc-500 dark:text-zinc-400">
            Transactions requiring auditor investigation
          </p>
        </div>
        <Link
          to="/transactions"
          className="inline-flex items-center gap-1.5 text-xs font-medium text-zinc-600 dark:text-zinc-300 hover:text-zinc-900 dark:hover:text-white transition-colors"
        >
          View all
          <ArrowRight className="w-3.5 h-3.5" />
        </Link>
      </div>

      <div className="overflow-x-auto">
        <table className="w-full text-left text-xs">
          <thead className="bg-zinc-50/75 dark:bg-zinc-900/50 border-b border-zinc-200/60 dark:border-zinc-800/60 text-zinc-500 dark:text-zinc-400">
            <tr>
              <th className="py-3 px-4 font-medium">Transaction ID</th>
              <th className="py-3 px-4 font-medium">Vendor</th>
              <th className="py-3 px-4 font-medium">Employee</th>
              <th className="py-3 px-4 font-medium text-right">Amount</th>
              <th className="py-3 px-4 font-medium text-center">Risk Score</th>
              <th className="py-3 px-4 font-medium text-center">Severity</th>
              <th className="py-3 px-4 font-medium text-right">Action</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-zinc-100 dark:divide-zinc-800/50">
            {transactions.map((tx) => (
              <tr
                key={tx.transactionId}
                className="hover:bg-zinc-50/80 dark:hover:bg-zinc-800/40 transition-colors group cursor-pointer"
              >
                <td className="py-3.5 px-4 font-mono font-medium text-zinc-900 dark:text-zinc-100">
                  <Link
                    to={`/transactions/${tx.transactionId}`}
                    className="hover:underline underline-offset-2 flex items-center gap-1.5"
                  >
                    {tx.transactionId}
                  </Link>
                </td>
                <td className="py-3.5 px-4 text-zinc-700 dark:text-zinc-300">
                  <div className="flex items-center gap-1.5 truncate max-w-[160px]">
                    <Building className="w-3.5 h-3.5 text-zinc-400 shrink-0" />
                    <span className="truncate">{tx.vendor}</span>
                  </div>
                </td>
                <td className="py-3.5 px-4 text-zinc-700 dark:text-zinc-300">
                  <div className="flex items-center gap-1.5 truncate max-w-[140px]">
                    <User className="w-3.5 h-3.5 text-zinc-400 shrink-0" />
                    <span>{tx.employee}</span>
                  </div>
                </td>
                <td className="py-3.5 px-4 font-mono font-medium text-right text-zinc-900 dark:text-zinc-100">
                  {formatCurrency(tx.amount)}
                </td>
                <td className="py-3.5 px-4 text-center">
                  <RiskScoreGauge score={tx.riskScore} size="sm" showLabel={false} />
                </td>
                <td className="py-3.5 px-4 text-center">
                  <RiskBadge severity={tx.riskLevel} />
                </td>
                <td className="py-3.5 px-4 text-right">
                  <Link
                    to={`/transactions/${tx.transactionId}`}
                    className="inline-flex items-center gap-1 px-2.5 py-1 rounded text-xs font-medium bg-zinc-100 dark:bg-zinc-800 text-zinc-700 dark:text-zinc-300 hover:bg-zinc-200 dark:hover:bg-zinc-700 transition-colors"
                  >
                    Investigate
                  </Link>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
