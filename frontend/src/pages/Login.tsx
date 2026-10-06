/**
 * Sprint 2B.2 – Login Page
 *
 * Polished AUREX authentication UI matching the existing design language.
 * No AppLayout (no sidebar/header) — a standalone full-screen centered experience.
 */

import React, { useState, useId } from 'react'
import { useNavigate } from 'react-router-dom'
import { Eye, EyeOff, Sun, Moon, AlertCircle, Loader2 } from 'lucide-react'
import { useAuth } from '../hooks/useAuth'
import { useTheme } from '../hooks/useTheme'
import { cn } from '../lib/utils'

// ── Helpers ───────────────────────────────────────────────────────────────────

/**
 * Map a raw error message from the auth layer into a safe, user-friendly string.
 * Never exposes stack traces, JWT details, or backend internals.
 */
function resolveErrorMessage(err: unknown): string {
  const raw = err instanceof Error ? err.message : String(err)
  const lower = raw.toLowerCase()

  // Invalid credentials (401 / "bad credentials" / "unauthorized")
  if (
    lower.includes('unauthorized') ||
    lower.includes('bad credentials') ||
    lower.includes('invalid username or password') ||
    lower.includes('401')
  ) {
    return 'Invalid username or password.'
  }

  // Network / server unreachable
  if (
    lower.includes('network error') ||
    lower.includes('failed to fetch') ||
    lower.includes('econnrefused') ||
    lower.includes('net::err') ||
    lower.includes('timeout')
  ) {
    return 'Unable to sign in right now. Please try again.'
  }

  // Generic server error
  if (lower.includes('500') || lower.includes('internal server')) {
    return 'A server error occurred. Please try again later.'
  }

  // Fallback — something went wrong but no specific pattern matched
  return 'Unable to sign in right now. Please try again.'
}

// ── AUREX Brand Logo (matches Sidebar exactly) ────────────────────────────────
const AurexLogo: React.FC<{ size?: number }> = ({ size = 40 }) => (
  <svg
    viewBox="0 0 32 32"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    width={size}
    height={size}
    aria-hidden="true"
  >
    <rect width="32" height="32" rx="8" className="fill-zinc-900 dark:fill-zinc-100" />
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
    <circle cx="16" cy="14.5" r="1.5" className="fill-white dark:fill-zinc-900" opacity="0.6" />
  </svg>
)

// ── Login Page ─────────────────────────────────────────────────────────────────
export const LoginPage: React.FC = () => {
  const { login } = useAuth()
  const { theme, toggleTheme } = useTheme()
  const navigate = useNavigate()

  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // Stable IDs for label/input association
  const usernameId = useId()
  const passwordId = useId()
  const errorId = useId()

  const isDark = theme === 'dark'

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    setError(null)

    // Client-side validation
    const trimmedUsername = username.trim()
    if (!trimmedUsername) {
      setError('Username is required.')
      return
    }
    if (!password) {
      setError('Password is required.')
      return
    }

    setIsSubmitting(true)
    try {
      await login(trimmedUsername, password)
      // Navigate only after auth state has been committed by login()
      navigate('/', { replace: true })
    } catch (err) {
      setError(resolveErrorMessage(err))
      // Keep username; clear password on failure (security best-practice)
      setPassword('')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="min-h-screen bg-zinc-50/50 dark:bg-[#0e0f12] flex flex-col antialiased">

      {/* ── Top bar: theme toggle ─────────────────────────────────────────── */}
      <div className="flex justify-end px-4 sm:px-6 py-4">
        <button
          id="login-theme-toggle"
          onClick={toggleTheme}
          aria-label={isDark ? 'Switch to light mode' : 'Switch to dark mode'}
          title={isDark ? 'Switch to light mode' : 'Switch to dark mode'}
          className="p-2 rounded-lg border border-zinc-200 dark:border-zinc-800 bg-zinc-50 dark:bg-zinc-900 text-zinc-500 dark:text-zinc-400 hover:text-zinc-900 dark:hover:text-zinc-100 hover:bg-zinc-100 dark:hover:bg-zinc-800/80 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-zinc-600 dark:focus-visible:outline-zinc-400 transition-colors"
        >
          {isDark ? (
            <Sun className="w-4 h-4 text-amber-400" aria-hidden="true" />
          ) : (
            <Moon className="w-4 h-4 text-slate-600" aria-hidden="true" />
          )}
        </button>
      </div>

      {/* ── Centered layout ───────────────────────────────────────────────── */}
      <div className="flex-1 flex items-center justify-center px-4 sm:px-6 pb-12">
        <div className="w-full max-w-sm">

          {/* ── Brand ─────────────────────────────────────────────────────── */}
          <div className="flex flex-col items-center mb-8">
            <AurexLogo size={40} />
            <div className="mt-4 text-center">
              <h1 className="text-xl font-bold tracking-wider text-zinc-900 dark:text-zinc-100">
                AUREX
              </h1>
              <p className="text-[11px] text-zinc-500 dark:text-zinc-400 tracking-tight mt-0.5">
                Financial Audit Risk Platform
              </p>
            </div>
          </div>

          {/* ── Card ──────────────────────────────────────────────────────── */}
          <div className="rounded-xl border border-zinc-200/80 dark:border-zinc-800/80 bg-white dark:bg-[#12141a] shadow-sm">

            {/* Card header */}
            <div className="px-6 pt-6 pb-5 border-b border-zinc-100 dark:border-zinc-800/60">
              <h2 className="text-sm font-semibold text-zinc-900 dark:text-zinc-100">
                Sign in to your account
              </h2>
              <p className="text-xs text-zinc-500 dark:text-zinc-400 mt-1 leading-relaxed">
                Secure access to financial risk investigations and audit intelligence.
              </p>
            </div>

            {/* Form */}
            <form
              onSubmit={handleSubmit}
              noValidate
              aria-label="Sign in"
              className="px-6 py-5 space-y-4"
            >
              {/* ── Error banner ──────────────────────────────────────────── */}
              {error && (
                <div
                  id={errorId}
                  role="alert"
                  aria-live="polite"
                  className="flex items-start gap-2.5 px-3.5 py-3 rounded-lg bg-rose-50 dark:bg-rose-500/10 border border-rose-200 dark:border-rose-500/20 text-rose-700 dark:text-rose-400"
                >
                  <AlertCircle
                    className="w-4 h-4 mt-0.5 shrink-0"
                    aria-hidden="true"
                  />
                  <span className="text-xs font-medium leading-snug">{error}</span>
                </div>
              )}

              {/* ── Username ───────────────────────────────────────────────── */}
              <div className="space-y-1.5">
                <label
                  htmlFor={usernameId}
                  className="block text-xs font-medium text-zinc-700 dark:text-zinc-300"
                >
                  Username
                </label>
                <input
                  id={usernameId}
                  type="text"
                  name="username"
                  autoComplete="username"
                  autoFocus
                  required
                  value={username}
                  onChange={(e) => {
                    setUsername(e.target.value)
                    if (error) setError(null)
                  }}
                  disabled={isSubmitting}
                  aria-required="true"
                  aria-describedby={error ? errorId : undefined}
                  placeholder="Enter your username"
                  className={cn(
                    'w-full px-3 py-2 rounded-lg text-sm border bg-white dark:bg-zinc-900/60',
                    'text-zinc-900 dark:text-zinc-100 placeholder:text-zinc-400 dark:placeholder:text-zinc-600',
                    'border-zinc-200 dark:border-zinc-700/80',
                    'focus:outline-none focus-visible:ring-2 focus-visible:ring-zinc-900 dark:focus-visible:ring-zinc-400 focus-visible:ring-offset-0',
                    'disabled:opacity-50 disabled:cursor-not-allowed',
                    'transition-colors'
                  )}
                />
              </div>

              {/* ── Password ──────────────────────────────────────────────── */}
              <div className="space-y-1.5">
                <label
                  htmlFor={passwordId}
                  className="block text-xs font-medium text-zinc-700 dark:text-zinc-300"
                >
                  Password
                </label>
                <div className="relative">
                  <input
                    id={passwordId}
                    type={showPassword ? 'text' : 'password'}
                    name="password"
                    autoComplete="current-password"
                    required
                    value={password}
                    onChange={(e) => {
                      setPassword(e.target.value)
                      if (error) setError(null)
                    }}
                    disabled={isSubmitting}
                    aria-required="true"
                    placeholder="Enter your password"
                    className={cn(
                      'w-full px-3 py-2 pr-10 rounded-lg text-sm border bg-white dark:bg-zinc-900/60',
                      'text-zinc-900 dark:text-zinc-100 placeholder:text-zinc-400 dark:placeholder:text-zinc-600',
                      'border-zinc-200 dark:border-zinc-700/80',
                      'focus:outline-none focus-visible:ring-2 focus-visible:ring-zinc-900 dark:focus-visible:ring-zinc-400 focus-visible:ring-offset-0',
                      'disabled:opacity-50 disabled:cursor-not-allowed',
                      'transition-colors'
                    )}
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword((prev) => !prev)}
                    disabled={isSubmitting}
                    aria-label={showPassword ? 'Hide password' : 'Show password'}
                    className="absolute inset-y-0 right-0 flex items-center px-3 text-zinc-400 dark:text-zinc-500 hover:text-zinc-700 dark:hover:text-zinc-300 disabled:opacity-40 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-900 dark:focus-visible:ring-zinc-400 rounded-r-lg"
                  >
                    {showPassword ? (
                      <EyeOff className="w-4 h-4" aria-hidden="true" />
                    ) : (
                      <Eye className="w-4 h-4" aria-hidden="true" />
                    )}
                  </button>
                </div>
              </div>

              {/* ── Submit ────────────────────────────────────────────────── */}
              <button
                id="login-submit"
                type="submit"
                disabled={isSubmitting}
                aria-disabled={isSubmitting}
                aria-busy={isSubmitting}
                className={cn(
                  'w-full flex items-center justify-center gap-2 px-4 py-2.5 mt-1 rounded-lg',
                  'text-sm font-medium transition-colors',
                  'bg-zinc-900 dark:bg-zinc-100 text-white dark:text-zinc-900',
                  'hover:bg-zinc-800 dark:hover:bg-zinc-200',
                  'focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-zinc-600 dark:focus-visible:outline-zinc-400',
                  'disabled:opacity-60 disabled:cursor-not-allowed'
                )}
              >
                {isSubmitting ? (
                  <>
                    <Loader2 className="w-4 h-4 animate-spin" aria-hidden="true" />
                    Signing in…
                  </>
                ) : (
                  'Sign in'
                )}
              </button>
            </form>
          </div>

          {/* ── Footer ────────────────────────────────────────────────────── */}
          <p className="text-center text-[11px] text-zinc-400 dark:text-zinc-600 mt-6">
            AUREX &copy; {new Date().getFullYear()} &mdash; Financial Audit Risk Platform
          </p>
        </div>
      </div>
    </div>
  )
}
