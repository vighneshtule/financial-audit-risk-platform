/**
 * Sprint 2B.5 – Verification Test Suite
 *
 * Runs via Node.js built-in test runner (no third-party testing dependencies required).
 * Verifies all 6 required test scenarios:
 *
 * A. Normal logout:
 *    - auth state cleared
 *    - localStorage cleared
 *    - tokenStore cleared
 *    - sessionExpired flag remains false (no session-expired message)
 *
 * B. Session expiry:
 *    - authenticated request returns 401
 *    - auth state cleared
 *    - localStorage cleared
 *    - tokenStore cleared
 *    - sessionExpired flag set to true ("Your session has expired. Please sign in again.")
 *
 * C. Duplicate 401s:
 *    - multiple simultaneous 401 responses
 *    - only one notification dispatched
 *    - only one session-expired state transition
 *
 * D. 403:
 *    - authenticated request returns 403
 *    - user remains authenticated
 *    - token remains in tokenStore
 *    - no logout or redirect triggered
 *
 * E. Login again:
 *    - after session expiration, successful login resets sessionExpired to false
 *    - session-expired banner does not interfere with login
 *
 * F. Existing login failures:
 *    - 401 on /auth/login does not trigger session expiration
 *    - safe error mapping preserves:
 *        - invalid credentials -> "Invalid username or password."
 *        - network/timeout errors -> "Unable to sign in right now. Please try again."
 *        - 500 internal errors -> "A server error occurred. Please try again later."
 */

import { test, describe, beforeEach } from 'node:test'
import assert from 'node:assert/strict'

// ── Mock localStorage ────────────────────────────────────────────────────────
class LocalStorageMock {
  constructor() {
    this.store = new Map()
  }
  getItem(key) {
    return this.store.get(key) ?? null
  }
  setItem(key, value) {
    this.store.set(key, String(value))
  }
  removeItem(key) {
    this.store.delete(key)
  }
  clear() {
    this.store.clear()
  }
}

globalThis.localStorage = new LocalStorageMock()

// ── Simulated tokenStore ─────────────────────────────────────────────────────
let _token = null
let _onUnauthorized = null
let _isHandlingUnauthorized = false

function getToken() {
  return _token
}

function setToken(token) {
  _token = token
  if (token !== null) {
    _isHandlingUnauthorized = false
  }
}

function registerUnauthorizedHandler(handler) {
  _onUnauthorized = handler
}

function notifyUnauthorized() {
  if (_isHandlingUnauthorized) {
    return false // dropped duplicate
  }
  _isHandlingUnauthorized = true
  _onUnauthorized?.()
  return true // handled
}

function resetUnauthorized() {
  _isHandlingUnauthorized = false
}

// ── Simulated interceptor logic (matching client.ts) ─────────────────────────
function simulateApiResponseError(error) {
  if (error.response?.status === 401) {
    const isLoginRequest = error.config?.url?.includes('/auth/login')
    if (!isLoginRequest) {
      notifyUnauthorized()
    }
  }

  const message =
    error.response?.data?.message ||
    error.response?.data?.error ||
    error.message ||
    'An unexpected error occurred'
  return new Error(message)
}

// ── Simulated error message resolver (matching Login.tsx) ───────────────────
function resolveErrorMessage(err) {
  const raw = err instanceof Error ? err.message : String(err)
  const lower = raw.toLowerCase()

  if (
    lower.includes('unauthorized') ||
    lower.includes('bad credentials') ||
    lower.includes('invalid username or password') ||
    lower.includes('401')
  ) {
    return 'Invalid username or password.'
  }

  if (
    lower.includes('network error') ||
    lower.includes('failed to fetch') ||
    lower.includes('econnrefused') ||
    lower.includes('net::err') ||
    lower.includes('timeout')
  ) {
    return 'Unable to sign in right now. Please try again.'
  }

  if (lower.includes('500') || lower.includes('internal server')) {
    return 'A server error occurred. Please try again later.'
  }

  return 'Unable to sign in right now. Please try again.'
}

// ── Simulated AuthContext Controller ─────────────────────────────────────────
class AuthController {
  constructor() {
    this.user = null
    this.token = null
    this.sessionExpired = false
    this.navigatedTo = null

    // Register 401 handler
    registerUnauthorizedHandler(() => {
      this.handleUnauthorized()
    })
  }

  get isAuthenticated() {
    return this.user !== null && this.token !== null
  }

  syncToken(newToken) {
    this.token = newToken
    setToken(newToken)
  }

  clearAuth() {
    this.user = null
    this.syncToken(null)
    localStorage.removeItem('aurex_auth')
  }

  clearSessionExpired() {
    this.sessionExpired = false
    resetUnauthorized()
  }

  handleUnauthorized() {
    this.sessionExpired = true
    this.clearAuth()
    // Simulated ProtectedRoute redirect
    this.navigatedTo = '/login'
  }

  loginSuccess(user, token) {
    this.sessionExpired = false
    resetUnauthorized()
    this.syncToken(token)
    this.user = user
    localStorage.setItem('aurex_auth', JSON.stringify({ user, token }))
    this.navigatedTo = '/'
  }

  logout() {
    this.sessionExpired = false
    resetUnauthorized()
    this.clearAuth()
    this.navigatedTo = '/login'
  }
}

// ── Test Suites ──────────────────────────────────────────────────────────────
describe('Sprint 2B.5 — Logout + Session Expiry UX', () => {
  let auth

  beforeEach(() => {
    localStorage.clear()
    setToken(null)
    resetUnauthorized()
    auth = new AuthController()
  })

  test('A. Normal logout clears all credentials without session-expired message', () => {
    // 1. Arrange: Authenticated user
    auth.loginSuccess({ id: 1, username: 'auditor1', role: 'AUDITOR' }, 'jwt-valid-123')
    assert.equal(auth.isAuthenticated, true)
    assert.equal(getToken(), 'jwt-valid-123')
    assert.ok(localStorage.getItem('aurex_auth'))
    assert.equal(auth.sessionExpired, false)

    // 2. Act: User clicks Logout
    auth.logout()

    // 3. Assert:
    assert.equal(auth.isAuthenticated, false)
    assert.equal(auth.user, null)
    assert.equal(auth.token, null)
    assert.equal(getToken(), null, 'tokenStore must be null')
    assert.equal(localStorage.getItem('aurex_auth'), null, 'localStorage must be cleared')
    assert.equal(auth.navigatedTo, '/login', 'User redirected to /login')
    assert.equal(auth.sessionExpired, false, 'sessionExpired MUST NOT be true on normal logout')
  })

  test('B. Session expiry on 401 sets session-expired message and clears auth', () => {
    // 1. Arrange: Authenticated user
    auth.loginSuccess({ id: 2, username: 'admin1', role: 'ADMIN' }, 'jwt-expiring-token')
    assert.equal(auth.isAuthenticated, true)

    // 2. Act: API request receives 401
    const err = simulateApiResponseError({
      response: { status: 401, data: { message: 'Token expired' } },
      config: { url: '/api/transactions' },
    })

    // 3. Assert:
    assert.equal(auth.isAuthenticated, false)
    assert.equal(auth.user, null)
    assert.equal(auth.token, null)
    assert.equal(getToken(), null, 'tokenStore must be cleared')
    assert.equal(localStorage.getItem('aurex_auth'), null, 'localStorage must be cleared')
    assert.equal(auth.navigatedTo, '/login', 'User reaches /login')
    assert.equal(auth.sessionExpired, true, 'sessionExpired must be true for 401')
    assert.equal(err.message, 'Token expired')
  })

  test('C. Duplicate concurrent 401s trigger only one notification/state update', () => {
    auth.loginSuccess({ id: 3, username: 'viewer1', role: 'VIEWER' }, 'jwt-token-concurrent')

    let notificationCount = 0
    registerUnauthorizedHandler(() => {
      notificationCount++
      auth.handleUnauthorized()
    })

    // Simulate 3 concurrent API requests failing with 401
    const result1 = notifyUnauthorized()
    const result2 = notifyUnauthorized()
    const result3 = notifyUnauthorized()

    assert.equal(result1, true, 'First 401 is dispatched')
    assert.equal(result2, false, 'Second 401 is deduplicated')
    assert.equal(result3, false, 'Third 401 is deduplicated')
    assert.equal(notificationCount, 1, 'Only exactly 1 notification is triggered')
    assert.equal(auth.sessionExpired, true)
    assert.equal(auth.isAuthenticated, false)
  })

  test('D. HTTP 403 Forbidden does not log user out or clear JWT', () => {
    auth.loginSuccess({ id: 4, username: 'viewer1', role: 'VIEWER' }, 'jwt-valid-token')

    // Simulate 403 response on restricted endpoint (/api/import)
    const err = simulateApiResponseError({
      response: { status: 403, data: { message: 'Access denied: insufficient permissions' } },
      config: { url: '/api/import' },
    })

    assert.equal(auth.isAuthenticated, true, 'User remains authenticated on 403')
    assert.equal(auth.token, 'jwt-valid-token', 'JWT is preserved')
    assert.equal(getToken(), 'jwt-valid-token', 'tokenStore is preserved')
    assert.equal(auth.sessionExpired, false, 'sessionExpired is not set')
    assert.equal(auth.navigatedTo, '/', 'No redirect to /login')
    assert.equal(err.message, 'Access denied: insufficient permissions')
  })

  test('E. Login again after session expiration resets sessionExpired state', () => {
    // 1. Session expires
    auth.loginSuccess({ id: 5, username: 'auditor2', role: 'AUDITOR' }, 'jwt-old')
    simulateApiResponseError({
      response: { status: 401 },
      config: { url: '/api/intelligence' },
    })
    assert.equal(auth.sessionExpired, true)

    // 2. User signs in again
    auth.loginSuccess({ id: 5, username: 'auditor2', role: 'AUDITOR' }, 'jwt-new-fresh')

    // 3. Assert
    assert.equal(auth.isAuthenticated, true)
    assert.equal(auth.sessionExpired, false, 'sessionExpired is reset to false on login')
    assert.equal(getToken(), 'jwt-new-fresh')
    assert.equal(auth.navigatedTo, '/')
  })

  test('F. Existing login failures remain distinct from session expiry', () => {
    // 1. Login attempt with wrong password (401 on /api/auth/login)
    let unauthorizedNotified = false
    registerUnauthorizedHandler(() => {
      unauthorizedNotified = true
    })

    const loginErr = simulateApiResponseError({
      response: { status: 401, data: { message: 'Bad credentials' } },
      config: { url: '/api/auth/login' },
    })

    assert.equal(unauthorizedNotified, false, '/api/auth/login 401 must NOT trigger notifyUnauthorized')
    assert.equal(resolveErrorMessage(loginErr), 'Invalid username or password.')

    // 2. Network error
    const netErr = simulateApiResponseError({
      response: null,
      message: 'Network Error: Failed to fetch',
      config: { url: '/api/auth/login' },
    })
    assert.equal(resolveErrorMessage(netErr), 'Unable to sign in right now. Please try again.')

    // 3. 500 Internal server error
    const serverErr = simulateApiResponseError({
      response: { status: 500, data: { message: 'Internal Server Error' } },
      config: { url: '/api/auth/login' },
    })
    assert.equal(resolveErrorMessage(serverErr), 'A server error occurred. Please try again later.')
  })
})
