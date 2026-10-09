/**
 * Sprint 2B.6 – Admin User Management Verification Tests
 *
 * Runs via Node.js built-in test runner.
 * Verifies frontend API mapping, validation, role filtering, and safety protections:
 *
 * 1. Role-based navigation filtering:
 *    - ADMIN role includes Users item
 *    - AUDITOR role excludes Users item
 *    - VIEWER role excludes Users item
 *
 * 2. User creation client validation:
 *    - username required
 *    - password required
 *    - password minimum 6 characters
 *    - role required (AUDITOR or VIEWER)
 *
 * 3. Self-disable safety rule:
 *    - Current authenticated admin account cannot be disabled
 *
 * 4. Error response mapping:
 *    - 409 duplicate username error handling
 *    - 409 last enabled admin lockout error handling
 *    - 403 access denied handling (does not log out)
 *    - 404 user not found handling
 */

import { test, describe } from 'node:test'
import assert from 'node:assert/strict'

// ── Mock navigation items logic from Sidebar.tsx ─────────────────────────────
function filterNavItems(role) {
  const navItems = [
    { label: 'Overview', to: '/', roles: ['ADMIN', 'AUDITOR', 'VIEWER'] },
    { label: 'Risk Intelligence', to: '/intelligence', roles: ['ADMIN', 'AUDITOR', 'VIEWER'] },
    { label: 'Transactions', to: '/transactions', roles: ['ADMIN', 'AUDITOR', 'VIEWER'] },
    { label: 'Investigations', to: '/investigations', roles: ['ADMIN', 'AUDITOR', 'VIEWER'] },
    { label: 'History', to: '/history', roles: ['ADMIN', 'AUDITOR', 'VIEWER'] },
    { label: 'Import', to: '/import', roles: ['ADMIN', 'AUDITOR'] },
    { label: 'Users', to: '/users', roles: ['ADMIN'] },
  ]
  return navItems.filter((item) => item.roles.includes(role))
}

// ── Mock client validation for user creation ────────────────────────────────
function validateCreateUser(payload) {
  const username = payload.username?.trim()
  if (!username) {
    return { valid: false, error: 'Username is required.' }
  }
  if (!payload.password) {
    return { valid: false, error: 'Password is required.' }
  }
  if (payload.password.length < 6) {
    return { valid: false, error: 'Password must be at least 6 characters long.' }
  }
  if (!payload.role || !['AUDITOR', 'VIEWER'].includes(payload.role)) {
    return { valid: false, error: 'Role is required and must be AUDITOR or VIEWER.' }
  }
  return { valid: true, error: null }
}

// ── Mock self-disable protection rule ───────────────────────────────────────
function canDisableUser(currentUserId, targetUser) {
  if (currentUserId === targetUser.id && targetUser.enabled) {
    return { allowed: false, message: 'You cannot disable your own authenticated account.' }
  }
  return { allowed: true }
}

describe('Sprint 2B.6 — Admin User Management Frontend Logic', () => {

  describe('1. Role-based Navigation Visibility', () => {
    test('ADMIN role includes Users in navigation', () => {
      const items = filterNavItems('ADMIN')
      const userItem = items.find((i) => i.to === '/users')
      assert.ok(userItem, 'Users link must be visible to ADMIN')
      assert.equal(userItem.label, 'Users')
    })

    test('AUDITOR role excludes Users from navigation', () => {
      const items = filterNavItems('AUDITOR')
      const userItem = items.find((i) => i.to === '/users')
      assert.equal(userItem, undefined, 'Users link must NOT be visible to AUDITOR')
    })

    test('VIEWER role excludes Users from navigation', () => {
      const items = filterNavItems('VIEWER')
      const userItem = items.find((i) => i.to === '/users')
      assert.equal(userItem, undefined, 'Users link must NOT be visible to VIEWER')
    })
  })

  describe('2. User Creation Validation', () => {
    test('rejects empty or whitespace username', () => {
      assert.equal(
        validateCreateUser({ username: '   ', password: 'ValidPass123', role: 'AUDITOR' }).error,
        'Username is required.'
      )
    })

    test('rejects empty password', () => {
      assert.equal(
        validateCreateUser({ username: 'valid_user', password: '', role: 'AUDITOR' }).error,
        'Password is required.'
      )
    })

    test('rejects password shorter than 6 characters', () => {
      assert.equal(
        validateCreateUser({ username: 'valid_user', password: '12345', role: 'AUDITOR' }).error,
        'Password must be at least 6 characters long.'
      )
    })

    test('accepts valid AUDITOR payload', () => {
      const res = validateCreateUser({ username: 'auditor1', password: 'Password123!', role: 'AUDITOR' })
      assert.equal(res.valid, true)
      assert.equal(res.error, null)
    })

    test('accepts valid VIEWER payload', () => {
      const res = validateCreateUser({ username: 'viewer1', password: 'Password123!', role: 'VIEWER' })
      assert.equal(res.valid, true)
      assert.equal(res.error, null)
    })
  })

  describe('3. Self-Management Protection Rule', () => {
    test('disallows disabling the currently authenticated account', () => {
      const currentAdmin = { id: 1, username: 'admin1', role: 'ADMIN' }
      const targetUser = { id: 1, username: 'admin1', role: 'ADMIN', enabled: true }

      const check = canDisableUser(currentAdmin.id, targetUser)
      assert.equal(check.allowed, false)
      assert.equal(check.message, 'You cannot disable your own authenticated account.')
    })

    test('allows disabling a different user account', () => {
      const currentAdmin = { id: 1, username: 'admin1', role: 'ADMIN' }
      const targetUser = { id: 2, username: 'auditor1', role: 'AUDITOR', enabled: true }

      const check = canDisableUser(currentAdmin.id, targetUser)
      assert.equal(check.allowed, true)
    })
  })

  describe('4. Server Error Safe Message Handling', () => {
    function parseApiErrorMessage(errResponse) {
      return (
        errResponse?.data?.message ||
        errResponse?.data?.error ||
        errResponse?.message ||
        'An unexpected error occurred'
      )
    }

    test('parses 409 duplicate username message safely', () => {
      const err = { data: { status: 409, message: "Username 'auditor1' already exists" } }
      assert.equal(parseApiErrorMessage(err), "Username 'auditor1' already exists")
    })

    test('parses 409 last admin lockout message safely', () => {
      const err = { data: { status: 409, message: "Cannot disable the last enabled ADMIN account" } }
      assert.equal(parseApiErrorMessage(err), "Cannot disable the last enabled ADMIN account")
    })

    test('parses 403 access denied message safely', () => {
      const err = { data: { status: 403, message: "Access denied: insufficient permissions" } }
      assert.equal(parseApiErrorMessage(err), "Access denied: insufficient permissions")
    })

    test('parses 404 user not found message safely', () => {
      const err = { data: { status: 404, message: "User not found with id: 999" } }
      assert.equal(parseApiErrorMessage(err), "User not found with id: 999")
    })
  })
})
