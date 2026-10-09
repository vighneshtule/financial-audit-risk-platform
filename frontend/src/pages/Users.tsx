/**
 * Sprint 2B.7 – Admin User Management Page (Polish)
 *
 * Additions over 2B.6:
 * - P1: Inline disable-confirmation row state (confirmingId)
 * - P2: Modal Escape-key and backdrop-click dismissal
 * - P3: Client-side search + role/status filter with no-match state
 * - P4: Success banner auto-dismiss (5 s), updatedAt column, aria-live,
 *       showPassword reset on modal close
 */

import React, { useCallback, useEffect, useRef, useState, useId } from 'react'
import {
  Users,
  UserPlus,
  RefreshCw,
  AlertCircle,
  CheckCircle2,
  X,
  Eye,
  EyeOff,
  Loader2,
  UserCheck,
  UserX,
  Search,
  FilterX,
} from 'lucide-react'
import { usersApi } from '../api/users'
import { useAuth } from '../hooks/useAuth'
import type { ManagedUser, UserRole } from '../types/auth'
import { ErrorState } from '../components/common/ErrorState'
import { EmptyState } from '../components/common/EmptyState'
import { Skeleton } from '../components/common/Skeleton'
import { cn } from '../lib/utils'

// ── Filter types ──────────────────────────────────────────────────────────────
type RoleFilter = 'ALL' | UserRole
type StatusFilter = 'ALL' | 'enabled' | 'disabled'

export const UsersPage: React.FC = () => {
  const { user: currentUser } = useAuth()

  const [users, setUsers] = useState<ManagedUser[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const [successMessage, setSuccessMessage] = useState<string | null>(null)

  // Per-row status-update loading state
  const [updatingId, setUpdatingId] = useState<number | null>(null)

  // P1: inline disable-confirmation state — only one row at a time
  const [confirmingId, setConfirmingId] = useState<number | null>(null)

  // P3: client-side search + filter state
  const [searchQuery, setSearchQuery] = useState('')
  const [roleFilter, setRoleFilter] = useState<RoleFilter>('ALL')
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('ALL')

  // Create User modal state
  const [showCreateModal, setShowCreateModal] = useState(false)
  const [newUsername, setNewUsername] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [newRole, setNewRole] = useState<UserRole>('AUDITOR')
  const [createSubmitting, setCreateSubmitting] = useState(false)
  const [createError, setCreateError] = useState<string | null>(null)

  const modalUsernameId = useId()
  const modalPasswordId = useId()
  const modalRoleId = useId()
  const searchId = useId()

  // ── P4: Success banner auto-dismiss (5 s) ───────────────────────────────────
  useEffect(() => {
    if (!successMessage) return
    const timer = setTimeout(() => setSuccessMessage(null), 5000)
    return () => clearTimeout(timer)
  }, [successMessage])

  // ── Data loading ─────────────────────────────────────────────────────────────
  const loadUsers = useCallback(async () => {
    setConfirmingId(null) // P1: dismiss pending confirmation on refresh
    try {
      setLoading(true)
      setError(null)
      const data = await usersApi.getUsers()
      setUsers(data)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unable to load users')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadUsers()
  }, [loadUsers])

  // ── P3: Derived filtered user list ───────────────────────────────────────────
  const filteredUsers = users.filter((u) => {
    const matchesSearch =
      searchQuery === '' ||
      u.username.toLowerCase().includes(searchQuery.toLowerCase())
    const matchesRole = roleFilter === 'ALL' || u.role === roleFilter
    const matchesStatus =
      statusFilter === 'ALL' ||
      (statusFilter === 'enabled' ? u.enabled : !u.enabled)
    return matchesSearch && matchesRole && matchesStatus
  })

  const hasActiveFilters =
    searchQuery !== '' || roleFilter !== 'ALL' || statusFilter !== 'ALL'

  const clearFilters = () => {
    setSearchQuery('')
    setRoleFilter('ALL')
    setStatusFilter('ALL')
  }

  // ── P1: Disable confirmation flow ────────────────────────────────────────────
  const handleDisableClick = (targetUser: ManagedUser) => {
    // Only for enabled users — disable requires confirmation
    setActionError(null)
    setSuccessMessage(null)
    setConfirmingId(targetUser.id)
  }

  const handleCancelConfirm = () => {
    setConfirmingId(null)
  }

  // Shared status-update executor — called by confirm (disable) and direct enable
  const executeStatusUpdate = async (targetUser: ManagedUser, newEnabled: boolean) => {
    setConfirmingId(null)
    setActionError(null)
    setSuccessMessage(null)

    // Client-side self-disable protection (ID-based AND username-based)
    const isSelf =
      (currentUser?.id != null && currentUser.id === targetUser.id) ||
      (currentUser?.username != null &&
        currentUser.username.toLowerCase() === targetUser.username.toLowerCase())

    if (isSelf && !newEnabled) {
      setActionError('You cannot disable your own authenticated account.')
      return
    }

    try {
      setUpdatingId(targetUser.id)
      const updated = await usersApi.updateUserStatus(targetUser.id, {
        enabled: newEnabled,
      })

      setUsers((prev) => prev.map((u) => (u.id === updated.id ? updated : u)))

      setSuccessMessage(
        `User '${updated.username}' is now ${updated.enabled ? 'enabled' : 'disabled'}.`
      )
    } catch (err) {
      setActionError(err instanceof Error ? err.message : 'Failed to update user status.')
    } finally {
      setUpdatingId(null)
    }
  }

  const handleConfirmDisable = (targetUser: ManagedUser) => {
    executeStatusUpdate(targetUser, false)
  }

  const handleEnableUser = (targetUser: ManagedUser) => {
    // Enable is immediate — no confirmation needed
    setConfirmingId(null) // dismiss any stale confirm state
    executeStatusUpdate(targetUser, true)
  }

  // ── P2: Modal close helper — guards against closing while submitting ─────────
  const closeModal = useCallback(() => {
    if (createSubmitting) return
    setShowCreateModal(false)
    setNewUsername('')
    setNewPassword('')
    setNewRole('AUDITOR')
    setShowPassword(false) // P4: reset password visibility on close
    setCreateError(null)
  }, [createSubmitting])

  // P2: Escape-key listener for modal
  const createSubmittingRef = useRef(createSubmitting)
  useEffect(() => {
    createSubmittingRef.current = createSubmitting
  }, [createSubmitting])

  useEffect(() => {
    if (!showCreateModal) return
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && !createSubmittingRef.current) {
        closeModal()
      }
    }
    document.addEventListener('keydown', handleKeyDown)
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [showCreateModal, closeModal])

  // ── Create User submit ────────────────────────────────────────────────────────
  const handleCreateSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setCreateError(null)

    const trimmedUsername = newUsername.trim()
    if (!trimmedUsername) {
      setCreateError('Username is required.')
      return
    }

    if (!newPassword) {
      setCreateError('Password is required.')
      return
    }

    if (newPassword.length < 6) {
      setCreateError('Password must be at least 6 characters long.')
      return
    }

    try {
      setCreateSubmitting(true)
      const created = await usersApi.createUser({
        username: trimmedUsername,
        password: newPassword,
        role: newRole,
      })

      // Full reset on success
      setNewUsername('')
      setNewPassword('')
      setNewRole('AUDITOR')
      setShowPassword(false) // P4: reset password visibility on success
      setShowCreateModal(false)
      setConfirmingId(null) // P1: dismiss any stale confirm on list refresh
      setSuccessMessage(`User '${created.username}' created successfully as ${created.role}.`)

      loadUsers()
    } catch (err) {
      setCreateError(err instanceof Error ? err.message : 'Failed to create user.')
    } finally {
      setCreateSubmitting(false)
    }
  }

  // ── Formatting helpers ────────────────────────────────────────────────────────
  const formatDateTime = (dateStr: string) => {
    if (!dateStr) return '—'
    try {
      const d = new Date(dateStr)
      if (isNaN(d.getTime())) return dateStr
      return d.toLocaleDateString(undefined, {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
      })
    } catch {
      return dateStr
    }
  }

  const renderRoleBadge = (role: UserRole) => {
    switch (role) {
      case 'ADMIN':
        return (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[11px] font-semibold bg-purple-500/10 text-purple-700 dark:text-purple-400 border border-purple-500/20">
            ADMIN
          </span>
        )
      case 'AUDITOR':
        return (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[11px] font-semibold bg-blue-500/10 text-blue-700 dark:text-blue-400 border border-blue-500/20">
            AUDITOR
          </span>
        )
      case 'VIEWER':
        return (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[11px] font-semibold bg-zinc-500/10 text-zinc-700 dark:text-zinc-400 border border-zinc-500/20">
            VIEWER
          </span>
        )
    }
  }

  // ── Render ────────────────────────────────────────────────────────────────────
  return (
    <div className="space-y-6">
      {/* ── Page Header ──────────────────────────────────────────────────────── */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-xl font-bold tracking-tight text-zinc-900 dark:text-zinc-100">
              User Management
            </h1>
            <span className="px-2 py-0.5 text-[10px] font-semibold rounded bg-zinc-100 dark:bg-zinc-800 text-zinc-600 dark:text-zinc-400 border border-zinc-200 dark:border-zinc-700">
              Admin Only
            </span>
          </div>
          <p className="text-xs text-zinc-500 dark:text-zinc-400 mt-1">
            Manage AUREX users and access roles.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={loadUsers}
            disabled={loading}
            aria-label="Refresh user list"
            title="Refresh"
            className="p-2 rounded-lg border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 text-zinc-600 dark:text-zinc-400 hover:text-zinc-900 dark:hover:text-zinc-100 hover:bg-zinc-50 dark:hover:bg-zinc-800/80 transition-colors cursor-pointer disabled:opacity-50"
          >
            <RefreshCw className={cn('w-4 h-4', loading && 'animate-spin')} />
          </button>

          <button
            id="create-user-btn"
            onClick={() => {
              setCreateError(null)
              setConfirmingId(null) // P1: dismiss confirm when modal opens
              setShowCreateModal(true)
            }}
            className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-lg text-xs font-medium bg-zinc-900 dark:bg-zinc-100 text-white dark:text-zinc-900 hover:bg-zinc-800 dark:hover:bg-zinc-200 transition-colors shadow-xs cursor-pointer"
          >
            <UserPlus className="w-3.5 h-3.5" />
            <span>Create user</span>
          </button>
        </div>
      </div>

      {/* ── Alerts ───────────────────────────────────────────────────────────── */}
      {actionError && (
        <div
          role="alert"
          aria-live="assertive"
          className="flex items-start justify-between gap-3 p-3.5 rounded-lg bg-rose-50 dark:bg-rose-500/10 border border-rose-200 dark:border-rose-500/20 text-rose-700 dark:text-rose-400 text-xs"
        >
          <div className="flex items-center gap-2">
            <AlertCircle className="w-4 h-4 shrink-0" />
            <span>{actionError}</span>
          </div>
          <button
            onClick={() => setActionError(null)}
            className="text-rose-500 hover:text-rose-700 dark:hover:text-rose-300"
            aria-label="Dismiss error"
          >
            <X className="w-3.5 h-3.5" />
          </button>
        </div>
      )}

      {successMessage && (
        <div
          role="status"
          aria-live="polite"
          className="flex items-start justify-between gap-3 p-3.5 rounded-lg bg-emerald-50 dark:bg-emerald-500/10 border border-emerald-200 dark:border-emerald-500/20 text-emerald-800 dark:text-emerald-400 text-xs"
        >
          <div className="flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 shrink-0" />
            <span>{successMessage}</span>
          </div>
          <button
            onClick={() => setSuccessMessage(null)}
            className="text-emerald-600 hover:text-emerald-800 dark:hover:text-emerald-300"
            aria-label="Dismiss message"
          >
            <X className="w-3.5 h-3.5" />
          </button>
        </div>
      )}

      {/* ── P3: Search + Filter Controls ─────────────────────────────────────── */}
      {users.length > 0 && (
        <div className="flex flex-col sm:flex-row gap-2">
          {/* Search */}
          <div className="relative flex-1">
            <label htmlFor={searchId} className="sr-only">
              Search users by username
            </label>
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-zinc-400 pointer-events-none" />
            <input
              id={searchId}
              type="search"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search by username…"
              className={cn(
                'w-full pl-8 pr-3 py-2 rounded-lg text-xs border bg-white dark:bg-zinc-900/60',
                'text-zinc-900 dark:text-zinc-100 placeholder:text-zinc-400 dark:placeholder:text-zinc-600',
                'border-zinc-200 dark:border-zinc-700/80',
                'focus:outline-none focus-visible:ring-2 focus-visible:ring-zinc-900 dark:focus-visible:ring-zinc-400'
              )}
            />
          </div>

          {/* Role filter */}
          <select
            value={roleFilter}
            onChange={(e) => setRoleFilter(e.target.value as RoleFilter)}
            aria-label="Filter by role"
            className={cn(
              'px-3 py-2 rounded-lg text-xs border bg-white dark:bg-zinc-900/60',
              'text-zinc-900 dark:text-zinc-100',
              'border-zinc-200 dark:border-zinc-700/80',
              'focus:outline-none focus-visible:ring-2 focus-visible:ring-zinc-900 dark:focus-visible:ring-zinc-400'
            )}
          >
            <option value="ALL">All roles</option>
            <option value="ADMIN">ADMIN</option>
            <option value="AUDITOR">AUDITOR</option>
            <option value="VIEWER">VIEWER</option>
          </select>

          {/* Status filter */}
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value as StatusFilter)}
            aria-label="Filter by status"
            className={cn(
              'px-3 py-2 rounded-lg text-xs border bg-white dark:bg-zinc-900/60',
              'text-zinc-900 dark:text-zinc-100',
              'border-zinc-200 dark:border-zinc-700/80',
              'focus:outline-none focus-visible:ring-2 focus-visible:ring-zinc-900 dark:focus-visible:ring-zinc-400'
            )}
          >
            <option value="ALL">All statuses</option>
            <option value="enabled">Enabled</option>
            <option value="disabled">Disabled</option>
          </select>

          {/* Clear filters button */}
          {hasActiveFilters && (
            <button
              type="button"
              onClick={clearFilters}
              aria-label="Clear all filters"
              className="inline-flex items-center gap-1.5 px-3 py-2 rounded-lg text-xs font-medium border border-zinc-200 dark:border-zinc-700 bg-white dark:bg-zinc-800/60 text-zinc-600 dark:text-zinc-400 hover:text-zinc-900 dark:hover:text-zinc-100 hover:bg-zinc-50 dark:hover:bg-zinc-700/60 transition-colors"
            >
              <FilterX className="w-3.5 h-3.5" />
              Clear
            </button>
          )}
        </div>
      )}

      {/* ── User Table / States ──────────────────────────────────────────────── */}
      {loading && users.length === 0 ? (
        <div className="rounded-xl border border-zinc-200/80 dark:border-zinc-800/80 bg-white dark:bg-[#12141a] p-4 space-y-3">
          <Skeleton className="h-8 w-full rounded-lg" />
          <Skeleton className="h-12 w-full rounded-lg" />
          <Skeleton className="h-12 w-full rounded-lg" />
          <Skeleton className="h-12 w-full rounded-lg" />
        </div>
      ) : error ? (
        <ErrorState
          title="Failed to load users"
          message={error}
          onRetry={loadUsers}
        />
      ) : users.length === 0 ? (
        /* True empty — no users in system */
        <EmptyState
          icon={<Users className="w-6 h-6" />}
          title="No users found"
          description="There are currently no users configured in the system."
          actionLabel="Create user"
          onAction={() => setShowCreateModal(true)}
        />
      ) : filteredUsers.length === 0 ? (
        /* Users exist but filters return nothing */
        <div className="flex flex-col items-center justify-center gap-3 p-10 rounded-xl border border-dashed border-zinc-300 dark:border-zinc-700 bg-zinc-50/50 dark:bg-zinc-900/30 text-center">
          <FilterX className="w-8 h-8 text-zinc-400 dark:text-zinc-500" />
          <div>
            <p className="text-sm font-semibold text-zinc-900 dark:text-zinc-100">
              No users match your filters
            </p>
            <p className="text-xs text-zinc-500 dark:text-zinc-400 mt-1">
              Try adjusting your search or filter criteria.
            </p>
          </div>
          <button
            type="button"
            onClick={clearFilters}
            className="inline-flex items-center gap-1.5 px-3.5 py-1.5 text-xs font-medium rounded-lg bg-zinc-900 dark:bg-zinc-100 text-white dark:text-zinc-900 hover:bg-zinc-800 dark:hover:bg-zinc-200 transition-colors"
          >
            Clear filters
          </button>
        </div>
      ) : (
        <div className="rounded-xl border border-zinc-200/80 dark:border-zinc-800/80 bg-white dark:bg-[#12141a] shadow-xs overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-zinc-50/75 dark:bg-zinc-900/50 border-b border-zinc-200/80 dark:border-zinc-800/80 text-zinc-500 dark:text-zinc-400">
                <tr>
                  <th className="py-3 px-4 font-medium">Username</th>
                  <th className="py-3 px-4 font-medium">Role</th>
                  <th className="py-3 px-4 font-medium">Status</th>
                  <th className="py-3 px-4 font-medium">Created</th>
                  {/* P4: updatedAt — hidden on xs, visible sm+ */}
                  <th className="hidden sm:table-cell py-3 px-4 font-medium">Last modified</th>
                  <th className="py-3 px-4 font-medium text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-100 dark:divide-zinc-800/60">
                {filteredUsers.map((u) => {
                  const isCurrentAuthUser =
                    (currentUser?.id != null && currentUser.id === u.id) ||
                    (currentUser?.username != null &&
                      currentUser.username.toLowerCase() === u.username.toLowerCase())
                  const isUpdating = updatingId === u.id
                  const isConfirming = confirmingId === u.id

                  return (
                    <tr
                      key={u.id}
                      className="hover:bg-zinc-50/80 dark:hover:bg-zinc-800/40 transition-colors"
                    >
                      <td className="py-3.5 px-4 font-medium text-zinc-900 dark:text-zinc-100">
                        <div className="flex items-center gap-2">
                          <span className="font-mono text-xs">{u.username}</span>
                          {isCurrentAuthUser && (
                            <span className="px-1.5 py-0.2 rounded text-[9px] font-semibold bg-amber-500/10 text-amber-700 dark:text-amber-400 border border-amber-500/20">
                              You
                            </span>
                          )}
                        </div>
                      </td>

                      <td className="py-3.5 px-4">
                        {renderRoleBadge(u.role)}
                      </td>

                      <td className="py-3.5 px-4">
                        {u.enabled ? (
                          <span className="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full text-[10px] font-medium bg-emerald-500/10 text-emerald-700 dark:text-emerald-400 border border-emerald-500/20">
                            <span className="w-1.5 h-1.5 rounded-full bg-emerald-500" />
                            Enabled
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full text-[10px] font-medium bg-rose-500/10 text-rose-700 dark:text-rose-400 border border-rose-500/20">
                            <span className="w-1.5 h-1.5 rounded-full bg-rose-500" />
                            Disabled
                          </span>
                        )}
                      </td>

                      <td className="py-3.5 px-4 text-zinc-500 dark:text-zinc-400">
                        {formatDateTime(u.createdAt)}
                      </td>

                      {/* P4: updatedAt column */}
                      <td className="hidden sm:table-cell py-3.5 px-4 text-zinc-500 dark:text-zinc-400">
                        {formatDateTime(u.updatedAt)}
                      </td>

                      <td className="py-3.5 px-4 text-right">
                        {isCurrentAuthUser ? (
                          <span
                            className="text-[11px] text-zinc-400 dark:text-zinc-500 italic select-none"
                            title="You cannot disable your currently authenticated account"
                          >
                            Active Session
                          </span>
                        ) : isUpdating ? (
                          /* Loading spinner while API request is in flight */
                          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-[11px] font-medium border border-zinc-200 dark:border-zinc-800 text-zinc-400 opacity-60">
                            <Loader2 className="w-3 h-3 animate-spin" />
                            <span>{u.enabled ? 'Disabling…' : 'Enabling…'}</span>
                          </span>
                        ) : isConfirming ? (
                          /* P1: Inline confirm / cancel */
                          <div className="inline-flex items-center gap-1.5">
                            <button
                              type="button"
                              onClick={() => handleConfirmDisable(u)}
                              aria-label={`Confirm disable user ${u.username}`}
                              className="inline-flex items-center gap-1 px-2.5 py-1 rounded-md text-[11px] font-medium transition-colors cursor-pointer border bg-rose-600 dark:bg-rose-500 text-white border-rose-600 dark:border-rose-500 hover:bg-rose-700 dark:hover:bg-rose-600"
                            >
                              <UserX className="w-3 h-3" />
                              <span>Confirm disable</span>
                            </button>
                            <button
                              type="button"
                              onClick={handleCancelConfirm}
                              aria-label={`Cancel disable for user ${u.username}`}
                              className="inline-flex items-center gap-1 px-2.5 py-1 rounded-md text-[11px] font-medium transition-colors cursor-pointer border border-zinc-200 dark:border-zinc-700 text-zinc-600 dark:text-zinc-400 hover:bg-zinc-50 dark:hover:bg-zinc-800/60"
                            >
                              <X className="w-3 h-3" />
                              <span>Cancel</span>
                            </button>
                          </div>
                        ) : u.enabled ? (
                          /* Disable button — first click starts confirmation */
                          <button
                            type="button"
                            onClick={() => handleDisableClick(u)}
                            aria-label={`Disable user ${u.username}`}
                            className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-[11px] font-medium transition-colors cursor-pointer border text-rose-600 dark:text-rose-400 hover:bg-rose-50 dark:hover:bg-rose-500/10 border-zinc-200 dark:border-zinc-800 hover:border-rose-200 dark:hover:border-rose-500/20"
                          >
                            <UserX className="w-3 h-3" />
                            <span>Disable</span>
                          </button>
                        ) : (
                          /* Enable button — immediate, no confirmation */
                          <button
                            type="button"
                            onClick={() => handleEnableUser(u)}
                            aria-label={`Enable user ${u.username}`}
                            className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-[11px] font-medium transition-colors cursor-pointer border text-emerald-600 dark:text-emerald-400 hover:bg-emerald-50 dark:hover:bg-emerald-500/10 border-zinc-200 dark:border-zinc-800 hover:border-emerald-200 dark:hover:border-emerald-500/20"
                          >
                            <UserCheck className="w-3 h-3" />
                            <span>Enable</span>
                          </button>
                        )}
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* ── Create User Modal ─────────────────────────────────────────────────── */}
      {showCreateModal && (
        <div
          /* P2: Backdrop click closes the modal */
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-xs"
          role="dialog"
          aria-modal="true"
          aria-labelledby="create-user-modal-title"
          onClick={closeModal}
        >
          {/* P2: Stop propagation so clicks inside the card don't bubble to backdrop */}
          <div
            className="w-full max-w-md rounded-xl border border-zinc-200/80 dark:border-zinc-800/80 bg-white dark:bg-[#12141a] shadow-xl overflow-hidden animate-in fade-in zoom-in-95 duration-150"
            onClick={(e) => e.stopPropagation()}
          >
            {/* Modal Header */}
            <div className="flex items-center justify-between px-6 py-4 border-b border-zinc-100 dark:border-zinc-800/60">
              <div>
                <h2
                  id="create-user-modal-title"
                  className="text-sm font-semibold text-zinc-900 dark:text-zinc-100"
                >
                  Create User
                </h2>
                <p className="text-xs text-zinc-500 dark:text-zinc-400 mt-0.5">
                  Add a new operational user with role-based access.
                </p>
              </div>
              <button
                type="button"
                onClick={closeModal}
                disabled={createSubmitting}
                className="p-1 rounded-md text-zinc-400 hover:text-zinc-600 dark:hover:text-zinc-200 transition-colors"
                aria-label="Close dialog"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {/* Modal Body / Form */}
            <form onSubmit={handleCreateSubmit} noValidate className="p-6 space-y-4">
              {createError && (
                <div
                  role="alert"
                  aria-live="assertive"
                  className="flex items-start gap-2 p-3 rounded-lg bg-rose-50 dark:bg-rose-500/10 border border-rose-200 dark:border-rose-500/20 text-rose-700 dark:text-rose-400 text-xs"
                >
                  <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
                  <span>{createError}</span>
                </div>
              )}

              {/* Username field */}
              <div className="space-y-1.5">
                <label
                  htmlFor={modalUsernameId}
                  className="block text-xs font-medium text-zinc-700 dark:text-zinc-300"
                >
                  Username
                </label>
                <input
                  id={modalUsernameId}
                  type="text"
                  required
                  autoFocus
                  disabled={createSubmitting}
                  value={newUsername}
                  onChange={(e) => {
                    setNewUsername(e.target.value)
                    if (createError) setCreateError(null)
                  }}
                  placeholder="e.g. auditor_alex"
                  className={cn(
                    'w-full px-3 py-2 rounded-lg text-xs border bg-white dark:bg-zinc-900/60',
                    'text-zinc-900 dark:text-zinc-100 placeholder:text-zinc-400 dark:placeholder:text-zinc-600',
                    'border-zinc-200 dark:border-zinc-700/80',
                    'focus:outline-none focus-visible:ring-2 focus-visible:ring-zinc-900 dark:focus-visible:ring-zinc-400',
                    'disabled:opacity-50'
                  )}
                />
              </div>

              {/* Password field */}
              <div className="space-y-1.5">
                <label
                  htmlFor={modalPasswordId}
                  className="block text-xs font-medium text-zinc-700 dark:text-zinc-300"
                >
                  Password <span className="text-zinc-400 font-normal">(min. 6 characters)</span>
                </label>
                <div className="relative">
                  <input
                    id={modalPasswordId}
                    type={showPassword ? 'text' : 'password'}
                    required
                    disabled={createSubmitting}
                    value={newPassword}
                    onChange={(e) => {
                      setNewPassword(e.target.value)
                      if (createError) setCreateError(null)
                    }}
                    placeholder="Enter secure temporary password"
                    className={cn(
                      'w-full px-3 py-2 pr-9 rounded-lg text-xs border bg-white dark:bg-zinc-900/60',
                      'text-zinc-900 dark:text-zinc-100 placeholder:text-zinc-400 dark:placeholder:text-zinc-600',
                      'border-zinc-200 dark:border-zinc-700/80',
                      'focus:outline-none focus-visible:ring-2 focus-visible:ring-zinc-900 dark:focus-visible:ring-zinc-400',
                      'disabled:opacity-50'
                    )}
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword((p) => !p)}
                    disabled={createSubmitting}
                    aria-label={showPassword ? 'Hide password' : 'Show password'}
                    className="absolute inset-y-0 right-0 flex items-center px-2.5 text-zinc-400 hover:text-zinc-600 dark:hover:text-zinc-200 transition-colors"
                  >
                    {showPassword ? (
                      <EyeOff className="w-3.5 h-3.5" />
                    ) : (
                      <Eye className="w-3.5 h-3.5" />
                    )}
                  </button>
                </div>
              </div>

              {/* Role selection */}
              <div className="space-y-1.5">
                <label
                  htmlFor={modalRoleId}
                  className="block text-xs font-medium text-zinc-700 dark:text-zinc-300"
                >
                  Access Role
                </label>
                <select
                  id={modalRoleId}
                  value={newRole}
                  disabled={createSubmitting}
                  onChange={(e) => setNewRole(e.target.value as UserRole)}
                  className={cn(
                    'w-full px-3 py-2 rounded-lg text-xs border bg-white dark:bg-zinc-900/60',
                    'text-zinc-900 dark:text-zinc-100',
                    'border-zinc-200 dark:border-zinc-700/80',
                    'focus:outline-none focus-visible:ring-2 focus-visible:ring-zinc-900 dark:focus-visible:ring-zinc-400',
                    'disabled:opacity-50'
                  )}
                >
                  <option value="AUDITOR">AUDITOR (Can audit transactions and risk findings)</option>
                  <option value="VIEWER">VIEWER (Read-only access to intelligence)</option>
                </select>
                <p className="text-[11px] text-zinc-500 dark:text-zinc-400">
                  {newRole === 'AUDITOR'
                    ? 'Auditors can import transactions, execute risk analyses, and submit audit decisions.'
                    : 'Viewers have read-only access to risk dashboards and transaction ledgers.'}
                </p>
              </div>

              {/* Modal Actions */}
              <div className="flex items-center justify-end gap-2 pt-3 border-t border-zinc-100 dark:border-zinc-800/60">
                <button
                  type="button"
                  onClick={closeModal}
                  disabled={createSubmitting}
                  className="px-3 py-2 rounded-lg text-xs font-medium border border-zinc-200 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-zinc-700 dark:text-zinc-300 hover:bg-zinc-50 dark:hover:bg-zinc-700/60 transition-colors cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  id="modal-submit-create-user"
                  type="submit"
                  disabled={createSubmitting}
                  className={cn(
                    'inline-flex items-center gap-1.5 px-4 py-2 rounded-lg text-xs font-medium',
                    'bg-zinc-900 dark:bg-zinc-100 text-white dark:text-zinc-900',
                    'hover:bg-zinc-800 dark:hover:bg-zinc-200 transition-colors shadow-xs cursor-pointer',
                    'disabled:opacity-60 disabled:cursor-not-allowed'
                  )}
                >
                  {createSubmitting ? (
                    <>
                      <Loader2 className="w-3.5 h-3.5 animate-spin" />
                      Creating…
                    </>
                  ) : (
                    'Create user'
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
