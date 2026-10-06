import React, { type ReactNode } from 'react'
import { ShieldAlert } from 'lucide-react'
import { useRoles } from '../../hooks/useRoles'
import type { UserRole } from '../../types/auth'

export interface AccessRestrictedProps {
  title?: string
  message?: string
}

export const AccessRestricted: React.FC<AccessRestrictedProps> = ({
  title = 'Access restricted',
  message = 'Your current role does not have permission to perform this action.',
}) => {
  return (
    <div className="p-8 max-w-md mx-auto my-12 rounded-xl border border-zinc-200/80 dark:border-zinc-800/80 bg-white dark:bg-[#14161b] shadow-2xs text-center space-y-4">
      <div className="w-12 h-12 rounded-xl bg-zinc-100 dark:bg-zinc-800 border border-zinc-200 dark:border-zinc-700 flex items-center justify-center text-zinc-500 dark:text-zinc-400 mx-auto">
        <ShieldAlert className="w-6 h-6" />
      </div>
      <div className="space-y-1">
        <h2 className="text-base font-bold text-zinc-900 dark:text-zinc-100 tracking-tight">
          {title}
        </h2>
        <p className="text-xs text-zinc-500 dark:text-zinc-400 leading-relaxed max-w-sm mx-auto">
          {message}
        </p>
      </div>
    </div>
  )
}

export interface RoleGuardProps {
  roles: UserRole[]
  children: ReactNode
  fallback?: ReactNode
}

export const RoleGuard: React.FC<RoleGuardProps> = ({
  roles,
  children,
  fallback = <AccessRestricted />,
}) => {
  const { hasAnyRole } = useRoles()

  if (!hasAnyRole(roles)) {
    return <>{fallback}</>
  }

  return <>{children}</>
}
