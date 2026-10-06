import { useAuth } from './useAuth'
import { hasRole as checkRole, hasAnyRole as checkAnyRole, formatRoleLabel } from '../lib/roleUtils'
import type { UserRole } from '../types/auth'

export function useRoles() {
  const { user } = useAuth()

  return {
    user,
    role: user?.role ?? null,
    roleLabel: formatRoleLabel(user?.role),
    hasRole: (role: UserRole) => checkRole(user, role),
    hasAnyRole: (roles: UserRole[]) => checkAnyRole(user, roles),
  }
}
