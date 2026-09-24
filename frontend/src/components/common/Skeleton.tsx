import React from 'react'
import { cn } from '../../lib/utils'

interface SkeletonProps {
  className?: string
}

export const Skeleton: React.FC<SkeletonProps> = ({ className }) => {
  return (
    <div
      className={cn(
        'animate-pulse rounded-md bg-zinc-200/80 dark:bg-zinc-800/80',
        className
      )}
    />
  )
}

export const MetricCardSkeleton: React.FC = () => {
  return (
    <div className="p-5 rounded-xl border border-zinc-200/70 dark:border-zinc-800/80 bg-white dark:bg-zinc-900 shadow-xs flex flex-col gap-3">
      <div className="flex items-center justify-between">
        <Skeleton className="h-3.5 w-24" />
        <Skeleton className="h-8 w-8 rounded-lg" />
      </div>
      <Skeleton className="h-7 w-20" />
      <Skeleton className="h-3 w-32" />
    </div>
  )
}

export const TableSkeleton: React.FC<{ rows?: number; cols?: number }> = ({
  rows = 5,
  cols = 6,
}) => {
  return (
    <div className="w-full space-y-3">
      <div className="flex items-center justify-between pb-3 border-b border-zinc-200 dark:border-zinc-800">
        <Skeleton className="h-4 w-32" />
        <Skeleton className="h-4 w-20" />
      </div>
      {Array.from({ length: rows }).map((_, i) => (
        <div
          key={i}
          className="flex items-center gap-4 py-3 border-b border-zinc-100 dark:border-zinc-800/50"
        >
          {Array.from({ length: cols }).map((_, j) => (
            <Skeleton
              key={j}
              className={cn(
                'h-4',
                j === 0 ? 'w-24' : j === 1 ? 'w-36' : j === 2 ? 'w-28' : 'w-20'
              )}
            />
          ))}
        </div>
      ))}
    </div>
  )
}
