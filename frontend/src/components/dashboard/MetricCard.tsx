import React from 'react'
import { motion } from 'framer-motion'
import { cn } from '../../lib/utils'

interface MetricCardProps {
  title: string
  value: string | number
  subtitle?: string
  icon?: React.ReactNode
  trend?: {
    value: string
    isPositive?: boolean
  }
  highlightColor?: 'default' | 'amber' | 'orange' | 'rose' | 'emerald'
  className?: string
}

export const MetricCard: React.FC<MetricCardProps> = ({
  title,
  value,
  subtitle,
  icon,
  highlightColor = 'default',
  className,
}) => {
  const colorStyles = {
    default: 'text-zinc-900 dark:text-zinc-100',
    emerald: 'text-emerald-600 dark:text-emerald-400',
    amber: 'text-amber-600 dark:text-amber-400',
    orange: 'text-orange-600 dark:text-orange-400',
    rose: 'text-rose-600 dark:text-rose-400',
  }[highlightColor]

  return (
    <motion.div
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.2 }}
      className={cn(
        'p-5 rounded-xl border border-zinc-200/80 dark:border-zinc-800/80 bg-white dark:bg-[#14161b] shadow-2xs hover:border-zinc-300 dark:hover:border-zinc-700/80 transition-all duration-150 flex flex-col justify-between',
        className
      )}
    >
      <div className="flex items-center justify-between gap-2 mb-3">
        <span className="text-xs font-medium text-zinc-500 dark:text-zinc-400 tracking-wide uppercase">
          {title}
        </span>
        {icon && (
          <div className="w-8 h-8 rounded-lg bg-zinc-100/80 dark:bg-zinc-800/80 border border-zinc-200/60 dark:border-zinc-700/50 flex items-center justify-center text-zinc-600 dark:text-zinc-400">
            {icon}
          </div>
        )}
      </div>

      <div>
        <div className={cn('text-2xl sm:text-3xl font-bold font-mono tracking-tight', colorStyles)}>
          {value}
        </div>
        {subtitle && (
          <p className="text-xs text-zinc-400 dark:text-zinc-500 mt-1 truncate">
            {subtitle}
          </p>
        )}
      </div>
    </motion.div>
  )
}
