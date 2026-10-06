/**
 * Sprint 2B.1 – Authentication Foundation
 *
 * useAuth() hook – consumes AuthContext.
 * Throws a clear developer error if used outside AuthProvider.
 */

import { useContext } from 'react'
import { AuthContext } from '../context/AuthContext'
import type { AuthContextValue } from '../types/auth'

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (context === undefined) {
    throw new Error(
      '[useAuth] must be used within an <AuthProvider>. ' +
        'Wrap your component tree with <AuthProvider> in App.tsx.'
    )
  }
  return context
}
