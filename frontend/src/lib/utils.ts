import { type ClassValue, clsx } from 'clsx'
import { twMerge } from 'tailwind-merge'

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

export function formatCurrency(amount: number | string | null | undefined): string {
  if (amount === null || amount === undefined) return 'INR 0'
  const num = typeof amount === 'string' ? parseFloat(amount) : amount
  if (isNaN(num)) return 'INR 0'
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 2,
  }).format(num).replace('₹', 'INR ')
}

export function formatDate(dateString: string | null | undefined): string {
  if (!dateString) return '—'
  try {
    const d = new Date(dateString)
    if (isNaN(d.getTime())) return dateString
    return new Intl.DateTimeFormat('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      hour12: true,
    }).format(d)
  } catch {
    return dateString
  }
}

export function formatRelativeTime(dateString: string | null | undefined): string {
  if (!dateString) return '—'
  try {
    const d = new Date(dateString)
    const now = new Date()
    const diffMs = now.getTime() - d.getTime()
    const diffMins = Math.round(diffMs / 60000)
    const diffHours = Math.round(diffMins / 60)
    const diffDays = Math.round(diffHours / 24)

    if (diffMins < 1) return 'Just now'
    if (diffMins < 60) return `${diffMins}m ago`
    if (diffHours < 24) return `${diffHours}h ago`
    if (diffDays < 7) return `${diffDays}d ago`
    return formatDate(dateString)
  } catch {
    return dateString
  }
}
