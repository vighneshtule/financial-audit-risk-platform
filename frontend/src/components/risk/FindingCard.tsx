import React from 'react'
import { motion } from 'framer-motion'
import {
  AlertTriangle,
  Clock,
  Copy,
  Coins,
  Activity,
  Layers,
  Info,
} from 'lucide-react'
import type { RiskFinding, RiskType } from '../../types'
import { RiskBadge } from '../common/RiskBadge'
import { cn } from '../../lib/utils'

interface FindingCardProps {
  finding: RiskFinding
  index?: number
}

export const FindingCard: React.FC<FindingCardProps> = ({ finding, index = 0 }) => {
  const getRuleMeta = (type: RiskType | string) => {
    switch (type) {
      case 'HIGH_AMOUNT':
        return {
          icon: AlertTriangle,
          name: 'High Transaction Amount',
          color: 'text-orange-500 bg-orange-500/10 border-orange-500/20',
        }
      case 'UNUSUAL_TRANSACTION_TIME':
        return {
          icon: Clock,
          name: 'Unusual Transaction Time',
          color: 'text-amber-500 bg-amber-500/10 border-amber-500/20',
        }
      case 'DUPLICATE_TRANSACTION':
        return {
          icon: Copy,
          name: 'Duplicate Transaction Pattern',
          color: 'text-rose-500 bg-rose-500/10 border-rose-500/20',
        }
      case 'ROUND_AMOUNT':
        return {
          icon: Coins,
          name: 'Round Amount Anomaly',
          color: 'text-purple-500 bg-purple-500/10 border-purple-500/20',
        }
      case 'TRANSACTION_VELOCITY':
        return {
          icon: Activity,
          name: 'High Transaction Velocity',
          color: 'text-blue-500 bg-blue-500/10 border-blue-500/20',
        }
      case 'VENDOR_CONCENTRATION':
        return {
          icon: Layers,
          name: 'Vendor Concentration',
          color: 'text-cyan-500 bg-cyan-500/10 border-cyan-500/20',
        }
      default:
        return {
          icon: Info,
          name: type || 'Risk Finding',
          color: 'text-zinc-500 bg-zinc-500/10 border-zinc-500/20',
        }
    }
  }

  const meta = getRuleMeta(finding.type)
  const Icon = meta.icon

  return (
    <motion.div
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.2, delay: index * 0.05 }}
      className="p-4 rounded-xl border border-zinc-200/80 dark:border-zinc-800/80 bg-white dark:bg-[#14161b] shadow-2xs hover:border-zinc-300 dark:hover:border-zinc-700 transition-colors flex flex-col justify-between"
    >
      <div className="flex items-start justify-between gap-3 mb-2">
        <div className="flex items-center gap-2.5">
          <div className={cn('w-7 h-7 rounded-lg border flex items-center justify-center', meta.color)}>
            <Icon className="w-4 h-4" />
          </div>
          <div>
            <h4 className="text-xs font-semibold text-zinc-900 dark:text-zinc-100">
              {meta.name}
            </h4>
            <span className="text-[10px] font-mono text-zinc-400 dark:text-zinc-500">
              {finding.type}
            </span>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <span className="px-2 py-0.5 rounded text-xs font-mono font-bold bg-zinc-100 dark:bg-zinc-800 text-zinc-800 dark:text-zinc-200 border border-zinc-200 dark:border-zinc-700">
            +{finding.score} pts
          </span>
          <RiskBadge severity={finding.severity} showDot={false} />
        </div>
      </div>

      <p className="text-xs text-zinc-600 dark:text-zinc-400 mt-2 leading-relaxed bg-zinc-50/60 dark:bg-zinc-900/60 p-2.5 rounded-lg border border-zinc-100 dark:border-zinc-800/60">
        {finding.explanation}
      </p>
    </motion.div>
  )
}
