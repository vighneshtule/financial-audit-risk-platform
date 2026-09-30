import React from 'react'
import { Menu, Sun, Moon, ShieldAlert } from 'lucide-react'
import { useTheme } from '../../hooks/useTheme'
import { Link } from 'react-router-dom'

interface HeaderProps {
  onMenuClick: () => void
}

export const Header: React.FC<HeaderProps> = ({ onMenuClick }) => {
  const { theme, toggleTheme } = useTheme()

  const isDark = theme === 'dark'
  const ariaLabel = isDark ? 'Switch to light mode' : 'Switch to dark mode'
  const tooltipTitle = ariaLabel

  return (
    <header className="sticky top-0 z-30 flex items-center justify-between h-16 px-4 sm:px-6 lg:px-8 border-b border-zinc-200/80 dark:border-zinc-800/80 bg-white/90 dark:bg-[#0e0f12]/90 backdrop-blur-md">
      <div className="flex items-center gap-3">
        <button
          onClick={onMenuClick}
          className="p-1.5 -ml-1.5 rounded-lg text-zinc-500 hover:text-zinc-900 dark:hover:text-zinc-100 hover:bg-zinc-100 dark:hover:bg-zinc-800 lg:hidden focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-zinc-600 dark:focus-visible:outline-zinc-400 transition-colors"
          aria-label="Open sidebar menu"
        >
          <Menu className="w-5 h-5" />
        </button>
        <div className="hidden sm:flex items-center gap-2 text-xs text-zinc-500 dark:text-zinc-400">
          <span className="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full bg-emerald-500/10 text-emerald-700 dark:text-emerald-400 border border-emerald-500/20 font-medium">
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-ping" />
            Live Engine Active
          </span>
        </div>
      </div>

      <div className="flex items-center gap-2">
        <Link
          to="/transactions"
          className="hidden md:inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium border border-zinc-200 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-zinc-700 dark:text-zinc-300 hover:bg-zinc-50 dark:hover:bg-zinc-700/60 transition-colors shadow-xs"
        >
          <ShieldAlert className="w-3.5 h-3.5 text-orange-500" />
          Review Flagged
        </Link>

        {/* ── Single theme toggle ──────────────────────────────────────────────
            • Dark mode active  → show Sun  → clicking switches to light
            • Light mode active → show Moon → clicking switches to dark
            One button, one icon, one source of truth (ThemeProvider context).
            ──────────────────────────────────────────────────────────────────── */}
        <button
          id="theme-toggle"
          onClick={toggleTheme}
          title={tooltipTitle}
          aria-label={ariaLabel}
          className="p-2 rounded-lg border border-zinc-200 dark:border-zinc-800 bg-zinc-50 dark:bg-zinc-900 text-zinc-500 dark:text-zinc-400 hover:text-zinc-900 dark:hover:text-zinc-100 hover:bg-zinc-100 dark:hover:bg-zinc-800/80 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-zinc-600 dark:focus-visible:outline-zinc-400 transition-colors"
        >
          {isDark ? (
            <Sun className="w-4 h-4 text-amber-400" aria-hidden="true" />
          ) : (
            <Moon className="w-4 h-4 text-slate-600" aria-hidden="true" />
          )}
        </button>
      </div>
    </header>
  )
}
