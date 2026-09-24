import React from 'react'
import { Link } from 'react-router-dom'
import {
  Building,
  User,
  Tag,
  ChevronRight,
} from 'lucide-react'
import type { RiskTransactionResponse } from '../../types'
import { RiskBadge } from '../common/RiskBadge'
import { RiskScoreGauge } from '../common/RiskScoreGauge'
import { formatCurrency } from '../../lib/utils'

interface TransactionTableProps {
  transactions: RiskTransactionResponse[]
  isLoading?: boolean
}

export const TransactionTable: React.FC<TransactionTableProps> = ({ transactions }) => {
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-left text-xs">
        <thead className="bg-zinc-50/75 dark:bg-zinc-900/50 border-b border-zinc-200/80 dark:border-zinc-800/80 text-zinc-500 dark:text-zinc-400">
          <tr>
            <th className="py-3 px-4 font-medium">Transaction ID</th>
            <th className="py-3 px-4 font-medium">Vendor</th>
            <th className="py-3 px-4 font-medium">Employee</th>
            <th className="py-3 px-4 font-medium">Category</th>
            <th className="py-3 px-4 font-medium text-right">Amount</th>
            <th className="py-3 px-4 font-medium text-center">Score</th>
            <th className="py-3 px-4 font-medium text-center">Risk Level</th>
            <th className="py-3 px-4 font-medium text-right"></th>
          </tr>
        </thead>
        <tbody className="divide-y divide-zinc-100 dark:divide-zinc-800/60">
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

              <td className="py-3.5 px-4 text-zinc-600 dark:text-zinc-400">
                <div className="flex items-center gap-1.5 truncate max-w-[130px]">
                  <Tag className="w-3 h-3 text-zinc-400 shrink-0" />
                  <span className="truncate">{tx.category || '—'}</span>
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
                  className="p-1.5 rounded-md inline-flex items-center text-zinc-400 hover:text-zinc-900 dark:hover:text-zinc-100 hover:bg-zinc-100 dark:hover:bg-zinc-800 transition-colors"
                  aria-label={`Investigate transaction ${tx.transactionId}`}
                >
                  <ChevronRight className="w-4 h-4" />
                </Link>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
