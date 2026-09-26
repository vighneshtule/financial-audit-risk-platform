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
          label: 'High Amount Policy',
          color: 'text-amber-500 bg-amber-500/10 border-amber-500/20',
          badgeColor: 'text-amber-600 dark:text-amber-400 bg-amber-500/10 border-amber-500/20',
        }
      case 'UNUSUAL_TRANSACTION_TIME':
        return {
          icon: Clock,
          label: 'Unusual Time Window',
          color: 'text-indigo-500 bg-indigo-500/10 border-indigo-500/20',
          badgeColor: 'text-indigo-600 dark:text-indigo-400 bg-indigo-500/10 border-indigo-500/20',
        }
      case 'DUPLICATE_TRANSACTION':
        return {
          icon: Copy,
          label: 'Duplicate Transaction Flag',
          color: 'text-rose-500 bg-rose-500/10 border-rose-500/20',
          badgeColor: 'text-rose-600 dark:text-rose-400 bg-rose-500/10 border-rose-500/20',
        }
      case 'ROUND_AMOUNT':
        return {
          icon: Coins,
          label: 'Round Value Anomaly',
          color: 'text-purple-500 bg-purple-500/10 border-purple-500/20',
          badgeColor: 'text-purple-600 dark:text-purple-400 bg-purple-500/10 border-purple-500/20',
        }
      case 'TRANSACTION_VELOCITY':
        return {
          icon: Activity,
          label: 'High Velocity Burst',
          color: 'text-blue-500 bg-blue-500/10 border-blue-500/20',
          badgeColor: 'text-blue-600 dark:text-blue-400 bg-blue-500/10 border-blue-500/20',
        }
      case 'VENDOR_CONCENTRATION':
        return {
          icon: Layers,
          label: 'Vendor Exposure Limit',
          color: 'text-teal-500 bg-teal-500/10 border-teal-500/20',
          badgeColor: 'text-teal-600 dark:text-teal-400 bg-teal-500/10 border-teal-500/20',
        }
      default:
        return {
          icon: Info,
          label: type ? type.replace(/_/g, ' ') : 'Audit Finding',
          color: 'text-zinc-500 bg-zinc-500/10 border-zinc-500/20',
          badgeColor: 'text-zinc-600 dark:text-zinc-400 bg-zinc-500/10 border-zinc-500/20',
        }
    }
  }

  const meta = getRuleMeta(finding.type)
  const Icon = meta.icon

  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3, delay: index * 0.06, ease: 'easeOut' }}
      className="group relative p-4 rounded-xl border border-zinc-200/90 dark:border-zinc-800/90 bg-white dark:bg-[#14161b] shadow-2xs hover:border-zinc-300 dark:hover:border-zinc-700 transition-all flex flex-col justify-between"
    >
      <div>
        <div className="flex items-start justify-between gap-3 mb-2.5">
          <div className="flex items-center gap-2.5">
            <div className={cn('w-7 h-7 rounded-lg border flex items-center justify-center shrink-0', meta.color)}>
              <Icon className="w-3.5 h-3.5" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="font-mono text-xs font-bold text-zinc-900 dark:text-zinc-100 tracking-tight">
                  {finding.type}
                </span>
                <span className="text-[10px] text-zinc-400 dark:text-zinc-500 hidden sm:inline">
                  • {meta.label}
                </span>
              </div>
            </div>
          </div>

          <div className="flex items-center gap-2 shrink-0">
            <span className="font-mono text-xs font-bold text-zinc-900 dark:text-zinc-100 px-2 py-0.5 rounded-md bg-zinc-100 dark:bg-zinc-800/80 border border-zinc-200/80 dark:border-zinc-700/80">
              +{finding.score}
            </span>
            <RiskBadge severity={finding.severity} showDot={false} className="text-[10px] px-2 py-0.5" />
          </div>
        </div>

        <p className="text-xs text-zinc-600 dark:text-zinc-300 leading-relaxed bg-zinc-50/70 dark:bg-zinc-900/60 p-3 rounded-lg border border-zinc-150 dark:border-zinc-800/60 font-sans">
          {finding.explanation}
        </p>
      </div>
    </motion.div>
  )
}
