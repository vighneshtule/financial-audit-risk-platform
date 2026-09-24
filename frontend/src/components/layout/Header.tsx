import React from 'react'
import { Menu, Sun, Moon, ShieldAlert } from 'lucide-react'
import { useTheme } from '../../hooks/useTheme'
import { Link } from 'react-router-dom'

interface HeaderProps {
  onMenuClick: () => void
}

export const Header: React.FC<HeaderProps> = ({ onMenuClick }) => {
  const { theme, toggleTheme } = useTheme()

  return (
    <header className="sticky top-0 z-30 flex items-center justify-between h-16 px-4 sm:px-6 lg:px-8 border-b border-zinc-200/80 dark:border-zinc-800/80 bg-white/80 dark:bg-[#0e0f12]/80 backdrop-blur-md">
      <div className="flex items-center gap-3">
        <button
          onClick={onMenuClick}
          className="p-1.5 -ml-1.5 rounded-lg text-zinc-500 hover:text-zinc-900 dark:hover:text-zinc-100 hover:bg-zinc-100 dark:hover:bg-zinc-800 lg:hidden"
          aria-label="Open sidebar menu"
        >
          <Menu className="w-5 h-5" />
        </button>
        <div className="hidden sm:flex items-center gap-2 text-xs text-zinc-500 dark:text-zinc-400">
          <span className="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-500/20 font-medium">
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-ping" />
            Live Engine Active
          </span>
        </div>
      </div>

      <div className="flex items-center gap-3">
        <Link
          to="/transactions"
          className="hidden md:inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium border border-zinc-200 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-zinc-700 dark:text-zinc-300 hover:bg-zinc-50 dark:hover:bg-zinc-700/60 transition-colors shadow-2xs"
        >
          <ShieldAlert className="w-3.5 h-3.5 text-orange-500" />
          Review Flagged
        </Link>

        {/* Theme quick toggle */}
        <button
          onClick={toggleTheme}
          className="p-2 rounded-lg border border-zinc-200 dark:border-zinc-800 bg-zinc-50 dark:bg-zinc-900 text-zinc-600 dark:text-zinc-400 hover:text-zinc-900 dark:hover:text-zinc-100 hover:bg-zinc-100 dark:hover:bg-zinc-800/80 transition-colors"
          aria-label="Toggle light and dark theme"
        >
          {theme === 'dark' ? <Moon className="w-4 h-4 text-zinc-300" /> : <Sun className="w-4 h-4 text-amber-500" />}
        </button>
      </div>
    </header>
  )
}
