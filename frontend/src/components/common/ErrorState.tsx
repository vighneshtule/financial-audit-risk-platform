import React from 'react'
import { AlertCircle, RefreshCw, ArrowLeft, Home } from 'lucide-react'
import { Link } from 'react-router-dom'
import { cn } from '../../lib/utils'

interface ErrorStateProps {
  title?: string
  message?: string
  onRetry?: () => void
  showBack?: boolean
  showHome?: boolean
  className?: string
}

export const ErrorState: React.FC<ErrorStateProps> = ({
  title = 'Unable to load data',
  message = 'An unexpected error occurred while communicating with the risk engine.',
  onRetry,
  showBack = false,
  showHome = true,
  className,
}) => {
  return (
    <div
      className={cn(
        'flex flex-col items-center justify-center p-8 text-center rounded-xl border border-zinc-200 dark:border-zinc-800 bg-white/50 dark:bg-zinc-900/50 backdrop-blur-sm',
        className
      )}
    >
      <div className="w-12 h-12 rounded-full bg-rose-500/10 dark:bg-rose-500/15 flex items-center justify-center text-rose-600 dark:text-rose-400 mb-4">
        <AlertCircle className="w-6 h-6" />
      </div>
      <h3 className="text-base font-semibold text-zinc-900 dark:text-zinc-100 mb-1">{title}</h3>
      <p className="text-sm text-zinc-500 dark:text-zinc-400 max-w-md mb-6">{message}</p>
      
      <div className="flex items-center gap-3">
        {onRetry && (
          <button
            onClick={onRetry}
            className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium rounded-lg bg-zinc-900 dark:bg-zinc-100 text-white dark:text-zinc-900 hover:bg-zinc-800 dark:hover:bg-zinc-200 transition-colors shadow-sm"
          >
            <RefreshCw className="w-4 h-4" />
            Retry
          </button>
        )}
        {showBack && (
          <button
            onClick={() => window.history.back()}
            className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium rounded-lg border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-zinc-700 dark:text-zinc-300 hover:bg-zinc-50 dark:hover:bg-zinc-700/50 transition-colors"
          >
            <ArrowLeft className="w-4 h-4" />
            Go back
          </button>
        )}
        {showHome && (
          <Link
            to="/"
            className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium rounded-lg border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-zinc-700 dark:text-zinc-300 hover:bg-zinc-50 dark:hover:bg-zinc-700/50 transition-colors"
          >
            <Home className="w-4 h-4" />
            Dashboard
          </Link>
        )}
      </div>
    </div>
  )
}
