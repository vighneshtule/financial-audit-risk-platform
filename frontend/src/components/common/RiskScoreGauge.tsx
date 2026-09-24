import React from 'react'
import { motion } from 'framer-motion'
import { cn } from '../../lib/utils'

interface RiskScoreGaugeProps {
  score: number
  size?: 'sm' | 'md' | 'lg' | 'xl'
  showLabel?: boolean
  className?: string
}

export const RiskScoreGauge: React.FC<RiskScoreGaugeProps> = ({
  score,
  size = 'md',
  showLabel = true,
  className,
}) => {
  const clampedScore = Math.max(0, Math.min(100, score))

  const getColor = (s: number) => {
    if (s >= 80) return { stroke: '#ef4444', text: 'text-rose-600 dark:text-rose-400' }
    if (s >= 60) return { stroke: '#f97316', text: 'text-orange-600 dark:text-orange-400' }
    if (s >= 30) return { stroke: '#f59e0b', text: 'text-amber-600 dark:text-amber-400' }
    return { stroke: '#10b981', text: 'text-emerald-600 dark:text-emerald-400' }
  }

  const dimensions = {
    sm: { size: 36, strokeWidth: 3.5, textSize: 'text-xs', labelSize: 'text-[9px]' },
    md: { size: 54, strokeWidth: 5, textSize: 'text-sm font-semibold', labelSize: 'text-[10px]' },
    lg: { size: 100, strokeWidth: 8, textSize: 'text-2xl font-bold', labelSize: 'text-xs' },
    xl: { size: 140, strokeWidth: 10, textSize: 'text-4xl font-extrabold', labelSize: 'text-sm' },
  }[size]

  const radius = (dimensions.size - dimensions.strokeWidth) / 2
  const circumference = 2 * Math.PI * radius
  const strokeDashoffset = circumference - (clampedScore / 100) * circumference
  const color = getColor(clampedScore)

  return (
    <div className={cn('relative inline-flex items-center justify-center flex-col', className)}>
      <svg
        width={dimensions.size}
        height={dimensions.size}
        className="transform -rotate-90"
      >
        <circle
          cx={dimensions.size / 2}
          cy={dimensions.size / 2}
          r={radius}
          stroke="currentColor"
          strokeWidth={dimensions.strokeWidth}
          className="text-zinc-200 dark:text-zinc-800"
          fill="transparent"
        />
        <motion.circle
          cx={dimensions.size / 2}
          cy={dimensions.size / 2}
          r={radius}
          stroke={color.stroke}
          strokeWidth={dimensions.strokeWidth}
          strokeDasharray={circumference}
          initial={{ strokeDashoffset: circumference }}
          animate={{ strokeDashoffset }}
          transition={{ duration: 0.8, ease: 'easeOut' }}
          strokeLinecap="round"
          fill="transparent"
        />
      </svg>
      <div className="absolute inset-0 flex flex-col items-center justify-center pointer-events-none">
        <span className={cn('font-mono leading-none', dimensions.textSize, color.text)}>
          {clampedScore}
        </span>
        {showLabel && size !== 'sm' && (
          <span className={cn('text-zinc-400 dark:text-zinc-500 uppercase tracking-wider mt-0.5', dimensions.labelSize)}>
            /100
          </span>
        )}
      </div>
    </div>
  )
}
