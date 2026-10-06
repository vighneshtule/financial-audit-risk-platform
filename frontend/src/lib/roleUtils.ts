import type { UserRole, AuthUser } from '../types/auth'

export const ROLE_LABELS: Record<UserRole, string> = {
  ADMIN: 'Administrator',
  AUDITOR: 'Auditor',
  VIEWER: 'Viewer',
}

/**
 * Formats a UserRole into a human-friendly display label.
 * Example: ADMIN -> Administrator, AUDITOR -> Auditor, VIEWER -> Viewer
 */
export function formatRoleLabel(role?: UserRole | null): string {
  if (!role) return ''
  return ROLE_LABELS[role] ?? role
}

/**
 * Checks if the given user has a specific role.
 */
export function hasRole(user: AuthUser | null, role: UserRole): boolean {
  return user?.role === role
}

/**
 * Checks if the given user has any of the specified roles.
 */
export function hasAnyRole(user: AuthUser | null, roles: UserRole[]): boolean {
  if (!user?.role) return false
  return roles.includes(user.role)
}
