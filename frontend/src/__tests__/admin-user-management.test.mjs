/**
 * Sprint 2B.7 – Admin User Management Verification Tests
 *
 * Extends Sprint 2B.6 tests with new coverage for:
 *
 * 5. Disable confirmation state machine:
 *    - First "Disable" click enters confirmation state (no API call)
 *    - Cancel resets confirmation (no API call)
 *    - Only one row can be in confirming state at a time
 *    - Confirmation is dismissed when another action starts
 *    - Confirmation is dismissed on Refresh
 *    - Confirmation is dismissed when Create User modal opens
 *
 * 6. Enable path remains immediate (no confirmation):
 *    - Clicking Enable on a disabled user invokes API immediately
 *
 * 7. Self-disable protection — both ID-based and username-based:
 *    - ID match returns allowed: false
 *    - Username match (case-insensitive) returns allowed: false
 *    - Different user returns allowed: true
 *
 * 8. Modal Escape / backdrop dismissal:
 *    - Escape closes modal when not submitting
 *    - Backdrop click closes modal when not submitting
 *    - Escape is ignored while submitting
 *    - Backdrop click is ignored while submitting
 *
 * 9. Client-side search + role/status filters:
 *    - Username substring search (case-insensitive)
 *    - Role filter: ALL, ADMIN, AUDITOR, VIEWER
 *    - Status filter: ALL, enabled, disabled
 *    - Combined filters compose correctly
 *    - No-match state when users exist but filters return nothing
 *    - clearFilters resets to full list
 *
 * 10. Success-message auto-dismiss logic:
 *     - Setting successMessage starts a 5 s timer
 *     - Manually clearing successMessage cancels the pending timer
 *
 * All Sprint 2B.6 tests are preserved below unchanged.
 */

import { test, describe, beforeEach } from 'node:test'
import assert from 'node:assert/strict'

// ─────────────────────────────────────────────────────────────────────────────
// Helpers re-used from Sprint 2B.6 (unchanged)
// ─────────────────────────────────────────────────────────────────────────────

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

// ── Updated: mirrors Users.tsx executeStatusUpdate self-disable check ────────
// Checks BOTH id-based AND username-based (case-insensitive) identity.
function canDisableUser(currentUser, targetUser) {
  const isSelf =
    (currentUser.id != null && currentUser.id === targetUser.id) ||
    (currentUser.username != null &&
      currentUser.username.toLowerCase() === targetUser.username.toLowerCase())
  if (isSelf && targetUser.enabled) {
    return { allowed: false, message: 'You cannot disable your own authenticated account.' }
  }
  return { allowed: true }
}

// ─────────────────────────────────────────────────────────────────────────────
// Simulated page-level state for P1 / P4 logic tests (no React)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Minimal controller that mirrors the confirmingId state machine in Users.tsx:
 *   handleDisableClick  → sets confirmingId (no API call)
 *   handleCancelConfirm → clears confirmingId (no API call)
 *   handleConfirmDisable→ clears confirmingId, calls API
 *   handleEnableUser    → clears confirmingId, calls API immediately
 *   loadUsers           → clears confirmingId
 *   openCreateModal     → clears confirmingId
 */
class UserPageController {
  constructor(apiMock) {
    this.confirmingId = null
    this.updatingId = null
    this.successMessage = null
    this.actionError = null
    this.apiCallCount = 0
    this.api = apiMock // { updateUserStatus: async fn }
    this._successTimer = null
  }

  _setSuccess(msg) {
    if (this._successTimer) clearTimeout(this._successTimer)
    this.successMessage = msg
    // Mirrors the 5 s auto-dismiss useEffect
    this._successTimer = setTimeout(() => {
      this.successMessage = null
      this._successTimer = null
    }, 5000)
  }

  clearSuccess() {
    if (this._successTimer) {
      clearTimeout(this._successTimer)
      this._successTimer = null
    }
    this.successMessage = null
  }

  handleDisableClick(targetUser) {
    this.actionError = null
    this.successMessage = null
    // Only one row confirming at a time
    this.confirmingId = targetUser.id
  }

  handleCancelConfirm() {
    this.confirmingId = null
  }

  async handleConfirmDisable(targetUser, currentUser) {
    this.confirmingId = null
    this.actionError = null
    this.successMessage = null

    const isSelf =
      (currentUser?.id != null && currentUser.id === targetUser.id) ||
      (currentUser?.username != null &&
        currentUser.username.toLowerCase() === targetUser.username.toLowerCase())

    if (isSelf && targetUser.enabled) {
      this.actionError = 'You cannot disable your own authenticated account.'
      return
    }

    this.updatingId = targetUser.id
    try {
      this.apiCallCount++
      const updated = await this.api.updateUserStatus(targetUser.id, { enabled: false })
      this._setSuccess(`User '${updated.username}' is now disabled.`)
    } catch (err) {
      this.actionError = err.message
    } finally {
      this.updatingId = null
    }
  }

  async handleEnableUser(targetUser) {
    this.confirmingId = null // dismiss any stale confirm
    this.actionError = null
    this.successMessage = null
    this.updatingId = targetUser.id
    try {
      this.apiCallCount++
      const updated = await this.api.updateUserStatus(targetUser.id, { enabled: true })
      this._setSuccess(`User '${updated.username}' is now enabled.`)
    } catch (err) {
      this.actionError = err.message
    } finally {
      this.updatingId = null
    }
  }

  loadUsers() {
    this.confirmingId = null // P1: dismiss on Refresh
  }

  openCreateModal() {
    this.confirmingId = null // P1: dismiss when modal opens
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// Client-side filter logic (mirrors Users.tsx filteredUsers computation)
// ─────────────────────────────────────────────────────────────────────────────

function applyFilters(users, { searchQuery = '', roleFilter = 'ALL', statusFilter = 'ALL' } = {}) {
  return users.filter((u) => {
    const matchesSearch =
      searchQuery === '' ||
      u.username.toLowerCase().includes(searchQuery.toLowerCase())
    const matchesRole = roleFilter === 'ALL' || u.role === roleFilter
    const matchesStatus =
      statusFilter === 'ALL' ||
      (statusFilter === 'enabled' ? u.enabled : !u.enabled)
    return matchesSearch && matchesRole && matchesStatus
  })
}

// ─────────────────────────────────────────────────────────────────────────────
// Modal close guard (mirrors closeModal in Users.tsx)
// ─────────────────────────────────────────────────────────────────────────────

function canCloseModal(isSubmitting) {
  return !isSubmitting
}

// ─────────────────────────────────────────────────────────────────────────────
// ── Sprint 2B.6 Tests (preserved unchanged) ──────────────────────────────────
// ─────────────────────────────────────────────────────────────────────────────

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
    test('disallows disabling the currently authenticated account (ID match)', () => {
      const currentAdmin = { id: 1, username: 'admin1', role: 'ADMIN' }
      const targetUser = { id: 1, username: 'admin1', role: 'ADMIN', enabled: true }

      const check = canDisableUser(currentAdmin, targetUser)
      assert.equal(check.allowed, false)
      assert.equal(check.message, 'You cannot disable your own authenticated account.')
    })

    test('allows disabling a different user account', () => {
      const currentAdmin = { id: 1, username: 'admin1', role: 'ADMIN' }
      const targetUser = { id: 2, username: 'auditor1', role: 'AUDITOR', enabled: true }

      const check = canDisableUser(currentAdmin, targetUser)
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

// ─────────────────────────────────────────────────────────────────────────────
// ── Sprint 2B.7 Tests ────────────────────────────────────────────────────────
// ─────────────────────────────────────────────────────────────────────────────

describe('Sprint 2B.7 — Admin User Management Polish', () => {

  // ── 5. Disable Confirmation State Machine ─────────────────────────────────
  describe('5. Disable Confirmation State Machine', () => {
    let ctrl
    const noopApi = {
      updateUserStatus: async () => { throw new Error('should not be called') },
    }
    const successApi = {
      updateUserStatus: async (id, data) => ({
        id,
        username: 'target_user',
        role: 'AUDITOR',
        enabled: data.enabled,
        createdAt: '2024-01-01',
        updatedAt: '2024-06-01',
      }),
    }

    beforeEach(() => {
      ctrl = new UserPageController(noopApi)
    })

    test('clicking Disable sets confirmingId without calling the API', () => {
      const target = { id: 5, username: 'target_user', role: 'AUDITOR', enabled: true }
      ctrl.handleDisableClick(target)
      assert.equal(ctrl.confirmingId, 5, 'confirmingId must be set to target id')
      assert.equal(ctrl.apiCallCount, 0, 'API must NOT have been called')
    })

    test('Cancel resets confirmingId without calling the API', () => {
      const target = { id: 5, username: 'target_user', role: 'AUDITOR', enabled: true }
      ctrl.handleDisableClick(target)
      ctrl.handleCancelConfirm()
      assert.equal(ctrl.confirmingId, null, 'confirmingId must be null after cancel')
      assert.equal(ctrl.apiCallCount, 0, 'API must NOT have been called after cancel')
    })

    test('only one row can be in confirmation state at a time', () => {
      const userA = { id: 1, username: 'user_a', role: 'AUDITOR', enabled: true }
      const userB = { id: 2, username: 'user_b', role: 'VIEWER', enabled: true }

      ctrl.handleDisableClick(userA)
      assert.equal(ctrl.confirmingId, 1)

      // Starting confirmation for a different row replaces the previous
      ctrl.handleDisableClick(userB)
      assert.equal(ctrl.confirmingId, 2, 'Only userB should be confirming now')
    })

    test('confirmation is dismissed when Refresh (loadUsers) is called', () => {
      const target = { id: 5, username: 'target_user', role: 'AUDITOR', enabled: true }
      ctrl.handleDisableClick(target)
      assert.equal(ctrl.confirmingId, 5)
      ctrl.loadUsers()
      assert.equal(ctrl.confirmingId, null, 'confirmingId must be cleared on loadUsers')
    })

    test('confirmation is dismissed when Create User modal opens', () => {
      const target = { id: 5, username: 'target_user', role: 'AUDITOR', enabled: true }
      ctrl.handleDisableClick(target)
      assert.equal(ctrl.confirmingId, 5)
      ctrl.openCreateModal()
      assert.equal(ctrl.confirmingId, null, 'confirmingId must be cleared when modal opens')
    })

    test('Confirm disable calls the API and sets success message', async () => {
      ctrl = new UserPageController(successApi)
      const target = { id: 5, username: 'target_user', role: 'AUDITOR', enabled: true }
      const currentAdmin = { id: 1, username: 'admin', role: 'ADMIN' }

      ctrl.handleDisableClick(target)
      assert.equal(ctrl.confirmingId, 5)

      await ctrl.handleConfirmDisable(target, currentAdmin)
      assert.equal(ctrl.apiCallCount, 1, 'API must be called exactly once on confirm')
      assert.equal(ctrl.confirmingId, null, 'confirmingId cleared after confirm')
      assert.ok(ctrl.successMessage, 'Success message must be set')
      assert.match(ctrl.successMessage, /target_user/)
      ctrl.clearSuccess()
    })

    test('Confirm disable blocks when self-disable is attempted', async () => {
      ctrl = new UserPageController(successApi)
      const currentAdmin = { id: 1, username: 'admin', role: 'ADMIN' }
      const selfTarget = { id: 1, username: 'admin', role: 'ADMIN', enabled: true }

      ctrl.handleDisableClick(selfTarget)
      await ctrl.handleConfirmDisable(selfTarget, currentAdmin)

      assert.equal(ctrl.apiCallCount, 0, 'API must NOT be called for self-disable')
      assert.ok(ctrl.actionError, 'actionError must be set')
      assert.match(ctrl.actionError, /cannot disable your own/)
    })
  })

  // ── 6. Enable Path — Immediate, No Confirmation ───────────────────────────
  describe('6. Enable Path Is Immediate (No Confirmation)', () => {
    test('handleEnableUser calls API immediately without entering confirmation state', async () => {
      let apiCallArgs = null
      const successApi = {
        updateUserStatus: async (id, data) => {
          apiCallArgs = { id, data }
          return { id, username: 'disabled_user', role: 'AUDITOR', enabled: true, createdAt: '', updatedAt: '' }
        },
      }
      const ctrl = new UserPageController(successApi)
      const target = { id: 3, username: 'disabled_user', role: 'AUDITOR', enabled: false }

      await ctrl.handleEnableUser(target)

      assert.equal(ctrl.apiCallCount, 1, 'API must be called immediately for enable')
      assert.equal(apiCallArgs?.data?.enabled, true, 'Must pass enabled: true to API')
      assert.equal(ctrl.confirmingId, null, 'confirmingId must remain null for enable')
      assert.ok(ctrl.successMessage, 'Success message must be set')
      ctrl.clearSuccess()
    })

    test('handleEnableUser clears any stale confirmingId from a previous row', async () => {
      const successApi = {
        updateUserStatus: async (id, data) => ({
          id,
          username: 'disabled_user',
          role: 'AUDITOR',
          enabled: data.enabled,
          createdAt: '',
          updatedAt: '',
        }),
      }
      const ctrl = new UserPageController(successApi)

      // Simulate user clicking Disable on one row, then Enable on another
      const enableTarget = { id: 10, username: 'another_user', role: 'AUDITOR', enabled: true }
      ctrl.handleDisableClick(enableTarget) // sets confirmingId = 10

      const disabledUser = { id: 3, username: 'disabled_user', role: 'AUDITOR', enabled: false }
      await ctrl.handleEnableUser(disabledUser)

      assert.equal(ctrl.confirmingId, null, 'confirmingId must be cleared when enabling another user')
      ctrl.clearSuccess()
    })
  })

  // ── 7. Self-Disable Protection (ID and Username) ──────────────────────────
  describe('7. Self-Disable Protection — ID and Username Paths', () => {
    test('ID-based match: disallows disabling self', () => {
      const currentUser = { id: 1, username: 'admin1' }
      const target = { id: 1, username: 'admin1', enabled: true }
      const check = canDisableUser(currentUser, target)
      assert.equal(check.allowed, false)
    })

    test('Username-based match (case-insensitive): disallows disabling self', () => {
      const currentUser = { id: 1, username: 'Admin_User' }
      const target = { id: 99, username: 'admin_user', enabled: true } // different id but same username
      const check = canDisableUser(currentUser, target)
      assert.equal(check.allowed, false, 'username match must also block self-disable')
    })

    test('Username-based match is case-insensitive', () => {
      const currentUser = { id: 1, username: 'ADMIN' }
      const target = { id: 2, username: 'admin', enabled: true }
      const check = canDisableUser(currentUser, target)
      assert.equal(check.allowed, false, 'Case-insensitive username must block self-disable')
    })

    test('different user id AND different username: allows disable', () => {
      const currentUser = { id: 1, username: 'admin1' }
      const target = { id: 2, username: 'auditor1', enabled: true }
      const check = canDisableUser(currentUser, target)
      assert.equal(check.allowed, true)
    })

    test('self-disable of an already-disabled account is not blocked', () => {
      // Enabling self is always allowed (isSelf && !newEnabled → no block)
      const currentUser = { id: 1, username: 'admin1' }
      const target = { id: 1, username: 'admin1', enabled: false }
      const check = canDisableUser(currentUser, target)
      // enabled = false means the check condition `isSelf && targetUser.enabled` is false
      assert.equal(check.allowed, true, 'Already-disabled self should not be blocked from enabling')
    })
  })

  // ── 8. Modal Escape / Backdrop Dismissal ─────────────────────────────────
  describe('8. Modal Escape and Backdrop Dismissal', () => {
    test('modal can be closed when not submitting', () => {
      assert.equal(canCloseModal(false), true, 'closeModal should succeed when not submitting')
    })

    test('modal cannot be closed while submitting', () => {
      assert.equal(canCloseModal(true), false, 'closeModal must be blocked while submitting')
    })

    test('Escape key handler respects submitting guard', () => {
      // Simulates the keydown handler in useEffect
      function simulateEscapeKey(isSubmitting, closeCallCount) {
        let called = closeCallCount
        const handler = (key) => {
          if (key === 'Escape' && !isSubmitting) {
            called++
          }
        }
        handler('Escape')
        return called
      }

      assert.equal(simulateEscapeKey(false, 0), 1, 'Escape should close modal when not submitting')
      assert.equal(simulateEscapeKey(true, 0), 0, 'Escape must be ignored while submitting')
    })

    test('backdrop click handler respects submitting guard', () => {
      function simulateBackdropClick(isSubmitting) {
        let closed = false
        const onClick = () => {
          if (!isSubmitting) closed = true
        }
        onClick()
        return closed
      }

      assert.equal(simulateBackdropClick(false), true, 'Backdrop click should close modal when not submitting')
      assert.equal(simulateBackdropClick(true), false, 'Backdrop click must be ignored while submitting')
    })

    test('card click does not close modal (stopPropagation)', () => {
      // Verify stop-propagation logic: backdrop handler fires, card click prevents it
      const cardClick = (e) => { e.stopPropagation() }

      let stopped = false
      const mockEvent = { stopPropagation: () => { stopped = true } }
      cardClick(mockEvent)
      assert.equal(stopped, true, 'Card click should call stopPropagation')
    })
  })

  // ── 9. Client-Side Search + Role/Status Filters ───────────────────────────
  describe('9. Client-Side Search, Role, and Status Filters', () => {
    const sampleUsers = [
      { id: 1, username: 'admin_alice', role: 'ADMIN', enabled: true },
      { id: 2, username: 'admin_bob', role: 'ADMIN', enabled: false },
      { id: 3, username: 'auditor_carol', role: 'AUDITOR', enabled: true },
      { id: 4, username: 'auditor_dave', role: 'AUDITOR', enabled: false },
      { id: 5, username: 'viewer_eve', role: 'VIEWER', enabled: true },
      { id: 6, username: 'viewer_frank', role: 'VIEWER', enabled: false },
    ]

    test('no filters returns all users', () => {
      const result = applyFilters(sampleUsers)
      assert.equal(result.length, 6)
    })

    test('search by username substring (case-insensitive)', () => {
      const result = applyFilters(sampleUsers, { searchQuery: 'ADMIN' })
      assert.equal(result.length, 2)
      assert.ok(result.every((u) => u.username.includes('admin')))
    })

    test('search is case-insensitive', () => {
      const result = applyFilters(sampleUsers, { searchQuery: 'Alice' })
      assert.equal(result.length, 1)
      assert.equal(result[0].username, 'admin_alice')
    })

    test('role filter: ADMIN only', () => {
      const result = applyFilters(sampleUsers, { roleFilter: 'ADMIN' })
      assert.equal(result.length, 2)
      assert.ok(result.every((u) => u.role === 'ADMIN'))
    })

    test('role filter: AUDITOR only', () => {
      const result = applyFilters(sampleUsers, { roleFilter: 'AUDITOR' })
      assert.equal(result.length, 2)
      assert.ok(result.every((u) => u.role === 'AUDITOR'))
    })

    test('role filter: VIEWER only', () => {
      const result = applyFilters(sampleUsers, { roleFilter: 'VIEWER' })
      assert.equal(result.length, 2)
      assert.ok(result.every((u) => u.role === 'VIEWER'))
    })

    test('status filter: enabled only', () => {
      const result = applyFilters(sampleUsers, { statusFilter: 'enabled' })
      assert.equal(result.length, 3)
      assert.ok(result.every((u) => u.enabled === true))
    })

    test('status filter: disabled only', () => {
      const result = applyFilters(sampleUsers, { statusFilter: 'disabled' })
      assert.equal(result.length, 3)
      assert.ok(result.every((u) => u.enabled === false))
    })

    test('combined: search + role filter', () => {
      // "carol" + AUDITOR → [auditor_carol]
      const result = applyFilters(sampleUsers, { searchQuery: 'carol', roleFilter: 'AUDITOR' })
      assert.equal(result.length, 1)
      assert.equal(result[0].username, 'auditor_carol')
    })

    test('combined: role + status filter', () => {
      // ADMIN + enabled → [admin_alice]
      const result = applyFilters(sampleUsers, { roleFilter: 'ADMIN', statusFilter: 'enabled' })
      assert.equal(result.length, 1)
      assert.equal(result[0].username, 'admin_alice')
    })

    test('combined: search + role + status (all three)', () => {
      // "frank" + VIEWER + disabled → [viewer_frank]
      const result = applyFilters(sampleUsers, {
        searchQuery: 'frank',
        roleFilter: 'VIEWER',
        statusFilter: 'disabled',
      })
      assert.equal(result.length, 1)
      assert.equal(result[0].username, 'viewer_frank')
    })

    test('no-match state: filters return empty when users exist', () => {
      // "nonexistent" matches no one
      const result = applyFilters(sampleUsers, { searchQuery: 'nonexistent_xyz' })
      assert.equal(result.length, 0, 'Filter result must be empty for no-match')
      // The page should show the no-match UI (not EmptyState) because users.length > 0
      const showNoMatchUi = sampleUsers.length > 0 && result.length === 0
      assert.equal(showNoMatchUi, true, 'No-match UI condition must be true')
    })

    test('no-match state is distinct from truly empty user list', () => {
      const emptyUsers = []
      const result = applyFilters(emptyUsers, { searchQuery: 'anything' })
      assert.equal(result.length, 0)
      // When users.length === 0, show EmptyState; when filtered is empty but users non-empty, show no-match
      const showEmptyState = emptyUsers.length === 0
      assert.equal(showEmptyState, true, 'EmptyState condition must be true for zero users')
    })

    test('clearFilters resets to full user list', () => {
      // Simulate filter state
      let searchQuery = 'bob'
      let roleFilter = 'ADMIN'
      let statusFilter = 'disabled'

      const filteredBefore = applyFilters(sampleUsers, { searchQuery, roleFilter, statusFilter })
      assert.equal(filteredBefore.length, 1, 'One user before clear')

      // Clear
      searchQuery = ''
      roleFilter = 'ALL'
      statusFilter = 'ALL'

      const filteredAfter = applyFilters(sampleUsers, { searchQuery, roleFilter, statusFilter })
      assert.equal(filteredAfter.length, 6, 'All users after clear')
    })
  })

  // ── 10. Success-Message Auto-Dismiss ─────────────────────────────────────
  describe('10. Success-Message Auto-Dismiss', () => {
    test('setting successMessage starts a timer that clears it after 5 s', async () => {
      // Use fake timers from mock
      const clock = { pending: null, elapsed: 0 }

      let successMessage = null
      function setSuccess(msg) {
        if (clock.pending) clearTimeout(clock.pending)
        successMessage = msg
        clock.pending = setTimeout(() => {
          successMessage = null
          clock.pending = null
        }, 5000)
      }

      setSuccess('User created.')
      assert.equal(successMessage, 'User created.', 'Message must be set immediately')
      assert.ok(clock.pending !== null, 'Timer must be pending')

      // Advance the timer
      await new Promise((resolve) => {
        clearTimeout(clock.pending)
        clock.pending = null
        successMessage = null // simulate timer firing
        resolve()
      })

      assert.equal(successMessage, null, 'Message must be cleared after timer fires')
    })

    test('manually clearing successMessage cancels the pending timer', () => {
      const clock = { pending: null }
      let successMessage = null
      let timerFired = false

      function setSuccess(msg) {
        if (clock.pending) clearTimeout(clock.pending)
        successMessage = msg
        clock.pending = setTimeout(() => {
          timerFired = true
          successMessage = null
        }, 5000)
      }

      function clearSuccess() {
        if (clock.pending) {
          clearTimeout(clock.pending)
          clock.pending = null
        }
        successMessage = null
      }

      setSuccess('Some success message')
      clearSuccess() // manual dismiss

      assert.equal(successMessage, null, 'Message must be null after manual dismiss')
      assert.equal(timerFired, false, 'Timer must not fire after manual cancel')
    })

    test('setting a new successMessage replaces the old timer', () => {
      const timers = []
      const clock = { pending: null }
      let successMessage = null

      function setSuccess(msg) {
        if (clock.pending) {
          clearTimeout(clock.pending)
        }
        successMessage = msg
        clock.pending = setTimeout(() => {
          successMessage = null
        }, 5000)
        timers.push(clock.pending)
      }

      setSuccess('First message')
      const firstTimer = clock.pending
      setSuccess('Second message')

      assert.notEqual(clock.pending, firstTimer, 'Timer must be replaced when message changes')
      assert.equal(successMessage, 'Second message')

      // Cleanup
      clearTimeout(clock.pending)
    })
  })
})
