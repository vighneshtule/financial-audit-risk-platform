/**
 * Sprint 2B.3 – Protected Routing
 *
 * ProtectedRoute
 * ──────────────
 * Wraps any set of React Router routes that require authentication.
 * Used as a layout route (renders <Outlet />) so it can protect an entire
 * nested route tree without duplicating guard logic per-route.
 *
 * Behaviour:
 *   isLoading  → minimal auth-loading indicator (no redirect, no flash)
 *   !isAuthenticated → <Navigate to="/login" replace />
 *   isAuthenticated  → <Outlet />   (renders child routes)
 *
 * PublicRoute
 * ───────────
 * Wraps public routes (currently only /login) so that already-authenticated
 * users are bounced back to the application root.
 *
 * Behaviour:
 *   isLoading        → minimal auth-loading indicator
 *   isAuthenticated  → <Navigate to="/" replace />
 *   !isAuthenticated → <Outlet />   (renders child routes)
 *
 * Loading state
 * ─────────────
 * Uses the same zinc/skeleton aesthetic as the rest of the AUREX design
 * system — a brief full-screen neutral backdrop while /api/auth/me resolves.
 * No arbitrary setTimeout, no focus traps, aria-live for screen readers.
 */

import React from 'react'
import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../../hooks/useAuth'

// ── Shared minimal loading screen ─────────────────────────────────────────────
// Matches the app's colour tokens exactly (see index.css + existing components).
// Intentionally bare — it appears only while /api/auth/me is in-flight.
const AuthLoadingScreen: React.FC = () => (
  <div
    className="min-h-screen bg-zinc-50/50 dark:bg-[#0e0f12] flex items-center justify-center"
    role="status"
    aria-live="polite"
    aria-label="Verifying authentication…"
  >
    {/* Subtle pulsing AUREX logo mark — no layout shift, no spinner noise */}
    <div className="flex flex-col items-center gap-4 select-none">
      <svg
        viewBox="0 0 32 32"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
        className="w-10 h-10 opacity-30 dark:opacity-20 animate-pulse"
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
      <span className="sr-only">Verifying authentication…</span>
    </div>
  </div>
)

// ── ProtectedRoute ─────────────────────────────────────────────────────────────
/**
 * Layout route that blocks access until authentication is confirmed.
 *
 * Usage in App.tsx (layout-route pattern):
 *
 *   <Route element={<ProtectedRoute />}>
 *     <Route path="/" element={<AppLayout />}>
 *       <Route index element={<Dashboard />} />
 *       …
 *     </Route>
 *   </Route>
 */
export const ProtectedRoute: React.FC = () => {
  const { isAuthenticated, isLoading } = useAuth()

  // Wait for AuthProvider to finish the /api/auth/me restoration.
  // Must NOT redirect during loading — the token may still be valid.
  if (isLoading) {
    return <AuthLoadingScreen />
  }

  if (!isAuthenticated) {
    // replace=true prevents the browser from pushing /login into history
    // as a navigation step, avoiding Back-button loops.
    return <Navigate to="/login" replace />
  }

  // Render the matched child route(s) via the standard outlet mechanism.
  return <Outlet />
}

// ── PublicRoute ────────────────────────────────────────────────────────────────
/**
 * Layout route for pages that should only be visible to unauthenticated users
 * (currently: /login).
 *
 * Usage in App.tsx:
 *
 *   <Route element={<PublicRoute />}>
 *     <Route path="/login" element={<LoginPage />} />
 *   </Route>
 */
export const PublicRoute: React.FC = () => {
  const { isAuthenticated, isLoading } = useAuth()

  // Wait for session restoration before deciding whether to redirect.
  if (isLoading) {
    return <AuthLoadingScreen />
  }

  if (isAuthenticated) {
    return <Navigate to="/" replace />
  }

  return <Outlet />
}
