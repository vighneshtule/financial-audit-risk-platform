import React from 'react'
import type { RiskSeverity } from '../../types'
import { cn } from '../../lib/utils'

interface RiskBadgeProps {
  severity: RiskSeverity | string
  className?: string
  showDot?: boolean
}

export const RiskBadge: React.FC<RiskBadgeProps> = ({
  severity,
  className,
  showDot = true,
}) => {
  const norm = (severity || 'LOW').toUpperCase()

  const config: Record<string, { bg: string; text: string; border: string; dot: string }> = {
    LOW: {
      bg: 'bg-emerald-500/10 dark:bg-emerald-500/15',
      text: 'text-emerald-700 dark:text-emerald-400',
      border: 'border-emerald-500/20 dark:border-emerald-500/30',
      dot: 'bg-emerald-500',
    },
    MEDIUM: {
      bg: 'bg-amber-500/10 dark:bg-amber-500/15',
      text: 'text-amber-700 dark:text-amber-400',
      border: 'border-amber-500/20 dark:border-amber-500/30',
      dot: 'bg-amber-500',
    },
    HIGH: {
      bg: 'bg-orange-500/10 dark:bg-orange-500/15',
      text: 'text-orange-700 dark:text-orange-400',
      border: 'border-orange-500/20 dark:border-orange-500/30',
      dot: 'bg-orange-500',
    },
    CRITICAL: {
      bg: 'bg-rose-500/10 dark:bg-rose-500/15',
      text: 'text-rose-700 dark:text-rose-400',
      border: 'border-rose-500/20 dark:border-rose-500/30',
      dot: 'bg-rose-500',
    },
  }

  const current = config[norm] || config.LOW

  return (
    <span
      className={cn(
        'inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-medium border tracking-wide uppercase',
        current.bg,
        current.text,
        current.border,
        className
      )}
    >
      {showDot && (
        <span className={cn('w-1.5 h-1.5 rounded-full animate-pulse', current.dot)} />
      )}
      {norm}
    </span>
  )
}
