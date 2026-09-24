import React from 'react'
import { FolderOpen, ArrowRight } from 'lucide-react'
import { Link } from 'react-router-dom'
import { cn } from '../../lib/utils'

interface EmptyStateProps {
  icon?: React.ReactNode
  title?: string
  description?: string
  actionLabel?: string
  actionLink?: string
  onAction?: () => void
  className?: string
}

export const EmptyState: React.FC<EmptyStateProps> = ({
  icon,
  title = 'No records found',
  description = 'There is currently no data matching your criteria.',
  actionLabel,
  actionLink,
  onAction,
  className,
}) => {
  return (
    <div
      className={cn(
        'flex flex-col items-center justify-center p-12 text-center rounded-xl border border-dashed border-zinc-300 dark:border-zinc-800 bg-zinc-50/50 dark:bg-zinc-900/30',
        className
      )}
    >
      <div className="w-12 h-12 rounded-xl bg-zinc-100 dark:bg-zinc-800/80 border border-zinc-200 dark:border-zinc-700/60 flex items-center justify-center text-zinc-400 dark:text-zinc-500 mb-4">
        {icon || <FolderOpen className="w-6 h-6" />}
      </div>
      <h3 className="text-sm font-semibold text-zinc-900 dark:text-zinc-100 mb-1">{title}</h3>
      <p className="text-xs text-zinc-500 dark:text-zinc-400 max-w-sm mb-5 leading-relaxed">
        {description}
      </p>

      {actionLink && actionLabel && (
        <Link
          to={actionLink}
          className="inline-flex items-center gap-1.5 px-3.5 py-1.5 text-xs font-medium rounded-lg bg-zinc-900 dark:bg-zinc-100 text-white dark:text-zinc-900 hover:bg-zinc-800 dark:hover:bg-zinc-200 transition-colors shadow-sm"
        >
          {actionLabel}
          <ArrowRight className="w-3.5 h-3.5" />
        </Link>
      )}

      {!actionLink && onAction && actionLabel && (
        <button
          onClick={onAction}
          className="inline-flex items-center gap-1.5 px-3.5 py-1.5 text-xs font-medium rounded-lg bg-zinc-900 dark:bg-zinc-100 text-white dark:text-zinc-900 hover:bg-zinc-800 dark:hover:bg-zinc-200 transition-colors shadow-sm"
        >
          {actionLabel}
          <ArrowRight className="w-3.5 h-3.5" />
        </button>
      )}
    </div>
  )
}
