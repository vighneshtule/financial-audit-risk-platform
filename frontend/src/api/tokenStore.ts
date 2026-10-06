/**
 * Sprint 2B.1 – Authentication Foundation
 *
 * tokenStore: a minimal module-level token getter/setter.
 *
 * Purpose: Break the circular dependency chain:
 *   AuthContext → apiClient → AuthContext
 *
 * Instead:
 *   AuthProvider calls setToken() when auth state changes.
 *   apiClient reads getToken() in its request interceptor.
 *   apiClient calls onUnauthorized() for 401 responses.
 *
 * No React imports – this is plain TypeScript.
 */

let _token: string | null = null
let _onUnauthorized: (() => void) | null = null

/** Called by the apiClient request interceptor to retrieve the current token. */
export function getToken(): string | null {
  return _token
}

/** Called by AuthProvider to update the token whenever auth state changes. */
export function setToken(token: string | null): void {
  _token = token
}

/**
 * Register a callback that the apiClient will invoke on a 401 response.
 * AuthProvider registers this on mount so it can clear auth state centrally.
 */
export function registerUnauthorizedHandler(handler: () => void): void {
  _onUnauthorized = handler
}

/**
 * Called by the apiClient response interceptor when a 401 is received.
 * Delegates to whatever handler AuthProvider registered.
 */
export function notifyUnauthorized(): void {
  _onUnauthorized?.()
}
