import React from 'react'
import { NavLink } from 'react-router-dom'
import {
  LayoutDashboard,
  ReceiptText,
  SearchCode,
  LineChart,
  History,
  UploadCloud,
  User,
  X,
} from 'lucide-react'
import { cn } from '../../lib/utils'

interface SidebarProps {
  isOpen?: boolean
  onClose?: () => void
}

export const Sidebar: React.FC<SidebarProps> = ({ isOpen, onClose }) => {
  const navItems = [
    { label: 'Overview', to: '/', icon: LayoutDashboard },
    { label: 'Risk Intelligence', to: '/intelligence', icon: LineChart },
    { label: 'Transactions', to: '/transactions', icon: ReceiptText },
    { label: 'Investigations', to: '/investigations', icon: SearchCode },
    { label: 'History', to: '/history', icon: History },
    { label: 'Import', to: '/import', icon: UploadCloud },
  ]

  return (
    <>
      {/* Mobile backdrop */}
      {isOpen && (
        <div
          className="fixed inset-0 z-40 bg-black/40 backdrop-blur-xs lg:hidden"
          onClick={onClose}
        />
      )}

      <aside
        className={cn(
          'fixed top-0 bottom-0 left-0 z-50 flex flex-col w-64 border-r border-zinc-200 dark:border-zinc-800/80 bg-white dark:bg-[#12141a] backdrop-blur-md transition-transform duration-200 ease-in-out lg:translate-x-0',
          isOpen ? 'translate-x-0' : '-translate-x-full'
        )}
      >
        {/* Brand Header */}
        <div className="flex items-center justify-between px-5 h-16 border-b border-zinc-200/80 dark:border-zinc-800/80">
          <NavLink to="/" className="flex items-center gap-3 group" onClick={onClose}>
            {/* AUREX SVG Logo */}
            <div className="w-8 h-8 flex-shrink-0">
              <svg viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg" className="w-full h-full">
                <rect width="32" height="32" rx="8" className="fill-zinc-900 dark:fill-zinc-100"/>
                {/* Stylized geometric A */}
                <path
                  d="M16 6L24.5 26H7.5L16 6Z"
                  className="fill-none stroke-white dark:stroke-zinc-900"
                  strokeWidth="2"
                  strokeLinejoin="round"
                />
                <path
                  d="M10.5 19H21.5"
                  className="stroke-white dark:stroke-zinc-900"
                  strokeWidth="1.75"
                  strokeLinecap="round"
                />
                {/* Shield accent dot */}
                <circle cx="16" cy="14.5" r="1.5" className="fill-white dark:fill-zinc-900" opacity="0.6"/>
              </svg>
            </div>
            <div>
              <div className="flex items-center gap-1.5">
                <span className="font-bold text-sm tracking-wider text-zinc-900 dark:text-zinc-100">
                  AUREX
                </span>
                <span className="px-1.5 py-0.5 rounded text-[9px] font-semibold bg-zinc-100 dark:bg-zinc-800 text-zinc-500 dark:text-zinc-400 border border-zinc-200 dark:border-zinc-700 leading-none">
                  MVP
                </span>
              </div>
              <p className="text-[10px] text-zinc-500 dark:text-zinc-400 tracking-tight mt-0.5">
                Financial Risk Intelligence
              </p>
            </div>
          </NavLink>

          {onClose && (
            <button
              onClick={onClose}
              className="p-1 rounded-md text-zinc-400 hover:text-zinc-600 dark:hover:text-zinc-200 lg:hidden"
              aria-label="Close sidebar"
            >
              <X className="w-5 h-5" />
            </button>
          )}
        </div>

        {/* Navigation items */}
        <div className="flex-1 px-3 py-4 space-y-0.5 overflow-y-auto">
          <div className="px-2 pb-3 text-[11px] font-medium tracking-wider text-zinc-400 dark:text-zinc-500 uppercase">
            Platform
          </div>
          {navItems.map((item) => {
            const Icon = item.icon
            return (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.to === '/'}
                onClick={onClose}
                className={({ isActive }) =>
                  cn(
                    'flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium transition-all duration-150',
                    isActive
                      ? 'bg-zinc-900 text-white dark:bg-zinc-100 dark:text-zinc-900 shadow-xs'
                      : 'text-zinc-600 dark:text-zinc-400 hover:text-zinc-900 dark:hover:text-zinc-100 hover:bg-zinc-100 dark:hover:bg-zinc-800/60'
                  )
                }
              >
                <Icon className="w-4 h-4" />
                {item.label}
              </NavLink>
            )
          })}
        </div>

        {/* Footer — User Profile only (no theme toggle here) */}
        <div className="p-3 border-t border-zinc-200/80 dark:border-zinc-800/80">
          <div className="flex items-center gap-3 px-3 py-2.5 rounded-lg bg-zinc-50 dark:bg-zinc-900/60 border border-zinc-200/60 dark:border-zinc-800/60">
            <div className="w-7 h-7 rounded-full bg-zinc-200 dark:bg-zinc-800 flex items-center justify-center text-zinc-600 dark:text-zinc-300">
              <User className="w-3.5 h-3.5" />
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-xs font-medium text-zinc-900 dark:text-zinc-100 truncate">
                Audit Lead
              </p>
              <p className="text-[10px] text-zinc-500 dark:text-zinc-400 truncate">
                Compliance Officer
              </p>
            </div>
          </div>
        </div>
      </aside>
    </>
  )
}
