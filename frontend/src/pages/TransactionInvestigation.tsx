import React, { useEffect, useState, useMemo, useCallback } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { motion, AnimatePresence } from 'framer-motion'
import {
  ArrowLeft,
  ShieldAlert,
  Play,
  History,
  CheckCircle2,
  Copy,
  Check,
  Building2,
  User,
  Calendar,
  Layers,
  Banknote,
  RotateCcw,
  GitBranch,
  ExternalLink,
  AlertTriangle,
  Scale,
  Send,
  Clock,
  FileText,
  Info,
  Lock,
} from 'lucide-react'
import { riskApi } from '../api/risk'
import { transactionsApi } from '../api/transactions'
import type {
  RiskReport,
  Transaction,
  RiskAnalysisHistoryItem,
  RiskFinding,
  TransactionContext,
  RelatedGroup,
  AuditDecision,
  AuditEvent,
  AuditDecisionType,
} from '../types'
import { RiskBadge } from '../components/common/RiskBadge'
import { RiskScoreGauge } from '../components/common/RiskScoreGauge'
import { FindingCard } from '../components/risk/FindingCard'
import { Skeleton } from '../components/common/Skeleton'
import { formatCurrency, formatDate, formatRelativeTime } from '../lib/utils'

// ── Audit Decision helpers ────────────────────────────────────────────────────

function decisionLabel(dtype: AuditDecisionType): string {
  switch (dtype) {
    case 'CONFIRMED_RISK':         return 'Confirmed Risk'
    case 'FALSE_POSITIVE':         return 'False Positive'
    case 'REQUIRES_INVESTIGATION': return 'Requires Investigation'
    case 'ESCALATED':              return 'Escalated'
  }
}

function decisionIcon(dtype: AuditDecisionType): string {
  switch (dtype) {
    case 'CONFIRMED_RISK':         return '⚠'
    case 'FALSE_POSITIVE':         return '✓'
    case 'REQUIRES_INVESTIGATION': return '🔍'
    case 'ESCALATED':              return '↑'
  }
}

function decisionDescription(dtype: AuditDecisionType): string {
  switch (dtype) {
    case 'CONFIRMED_RISK':         return 'Flag as a genuine risk finding'
    case 'FALSE_POSITIVE':         return 'Mark as incorrectly flagged'
    case 'REQUIRES_INVESTIGATION': return 'Needs further review'
    case 'ESCALATED':              return 'Escalate to senior review'
  }
}

function decisionActiveStyle(dtype: AuditDecisionType): string {
  switch (dtype) {
    case 'CONFIRMED_RISK':
      return 'border-red-400 dark:border-red-600 bg-red-50 dark:bg-red-950/30 text-red-800 dark:text-red-200 ring-1 ring-red-400/40'
    case 'FALSE_POSITIVE':
      return 'border-emerald-400 dark:border-emerald-600 bg-emerald-50 dark:bg-emerald-950/30 text-emerald-800 dark:text-emerald-200 ring-1 ring-emerald-400/40'
    case 'REQUIRES_INVESTIGATION':
      return 'border-amber-400 dark:border-amber-600 bg-amber-50 dark:bg-amber-950/30 text-amber-800 dark:text-amber-200 ring-1 ring-amber-400/40'
    case 'ESCALATED':
      return 'border-purple-400 dark:border-purple-600 bg-purple-50 dark:bg-purple-950/30 text-purple-800 dark:text-purple-200 ring-1 ring-purple-400/40'
  }
}

const decisionBadgeStyle: Record<AuditDecisionType, string> = {
  CONFIRMED_RISK:          'bg-red-500/10 text-red-700 dark:text-red-300 border-red-400/30',
  FALSE_POSITIVE:          'bg-emerald-500/10 text-emerald-700 dark:text-emerald-300 border-emerald-400/30',
  REQUIRES_INVESTIGATION:  'bg-amber-500/10 text-amber-700 dark:text-amber-300 border-amber-400/30',
  ESCALATED:               'bg-purple-500/10 text-purple-700 dark:text-purple-300 border-purple-400/30',
}

const AuditDecisionBadge: React.FC<{ decision: AuditDecisionType; className?: string }> = ({
  decision,
  className = '',
}) => (
  <span
    className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-semibold border uppercase tracking-wide ${decisionBadgeStyle[decision]} ${className}`}
  >
    <span aria-hidden="true">{decisionIcon(decision)}</span>
    {decisionLabel(decision)}
  </span>
)

// ── Main page ─────────────────────────────────────────────────────────────────

export const TransactionInvestigation: React.FC = () => {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()

  // Core data
  const [transaction, setTransaction] = useState<Transaction | null>(null)
  const [report, setReport] = useState<RiskReport | null>(null)
  const [historyRuns, setHistoryRuns] = useState<RiskAnalysisHistoryItem[]>([])
  const [selectedRunId, setSelectedRunId] = useState<number | null>(null)
  const [loading, setLoading] = useState(true)
  const [analyzing, setAnalyzing] = useState(false)
  const [copied, setCopied] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [historyLoading, setHistoryLoading] = useState(false)

  // Evidence
  const [evidenceContext, setEvidenceContext] = useState<TransactionContext | null>(null)
  const [evidenceLoading, setEvidenceLoading] = useState<boolean>(true)
  const [evidenceError, setEvidenceError] = useState<string | null>(null)

  // Audit decision
  const [latestDecision, setLatestDecision] = useState<AuditDecision | null>(null)
  const [auditEvents, setAuditEvents] = useState<AuditEvent[]>([])
  const [auditLoading, setAuditLoading] = useState(false)
  const [auditSubmitting, setAuditSubmitting] = useState(false)
  const [auditError, setAuditError] = useState<string | null>(null)
  const [auditSuccess, setAuditSuccess] = useState(false)
  const [auditForm, setAuditForm] = useState<{
    decision: AuditDecisionType | ''
    comment: string
    decidedBy: string
  }>({ decision: '', comment: '', decidedBy: '' })

  // ── Loaders ──────────────────────────────────────────────────────────────────

  const loadAuditData = useCallback(async (txnId: string) => {
    try {
      setAuditLoading(true)
      setAuditError(null)
      const [decision, events] = await Promise.all([
        riskApi.getLatestAuditDecision(txnId),
        riskApi.getAuditEvents(txnId).catch(() => [] as AuditEvent[]),
      ])
      setLatestDecision(decision)
      setAuditEvents(events)
    } catch {
      // Audit data is supplementary; silent fail
    } finally {
      setAuditLoading(false)
    }
  }, [])

  const loadEvidence = useCallback(async (txnId: string) => {
    try {
      setEvidenceLoading(true)
      setEvidenceError(null)
      const ctx = await riskApi.getTransactionContext(txnId)
      setEvidenceContext(ctx)
    } catch (err) {
      setEvidenceError(err instanceof Error ? err.message : 'Failed to load evidence context')
    } finally {
      setEvidenceLoading(false)
    }
  }, [])

  const loadInvestigation = useCallback(async () => {
    if (!id) return
    try {
      setLoading(true)
      setError(null)
      setSelectedRunId(null)

      const [reportData, historyData, transactionsList] = await Promise.all([
        riskApi.analyzeTransaction(id),
        riskApi.getTransactionHistory(id).catch(() => ({ transactionId: id, analysisRuns: [] })),
        transactionsApi.getAll().catch(() => []),
      ])

      setReport(reportData)
      setHistoryRuns(historyData.analysisRuns || [])

      const match = transactionsList.find((t) => t.id === id)
      if (match) {
        setTransaction(match)
      } else {
        setError(`Transaction ${id} not found in repository`)
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : `Could not load transaction ${id}`)
    } finally {
      setLoading(false)
    }
  }, [id])

  useEffect(() => {
    loadInvestigation()
    if (id) {
      loadEvidence(id)
      loadAuditData(id)
    }
  }, [id])

  // ── Handlers ─────────────────────────────────────────────────────────────────

  const handleCopyId = () => {
    if (!id) return
    navigator.clipboard.writeText(id)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  const handleRunAnalysis = async () => {
    if (!id) return
    try {
      setAnalyzing(true)
      const persistedReport = await riskApi.analyzeAndPersist(id)
      setReport(persistedReport)
      const historyData = await riskApi.getTransactionHistory(id)
      setHistoryRuns(historyData.analysisRuns || [])
      setSelectedRunId(null)
      loadEvidence(id)
      loadAuditData(id)
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Analysis execution failed')
    } finally {
      setAnalyzing(false)
    }
  }

  const handleSelectHistoricalRun = async (runId: number) => {
    if (!id) return
    try {
      if (selectedRunId === runId) {
        setSelectedRunId(null)
        setHistoryLoading(true)
        const liveReport = await riskApi.analyzeTransaction(id)
        setReport(liveReport)
        return
      }
      setHistoryLoading(true)
      setSelectedRunId(runId)
      const historicalRun = await riskApi.getHistoryRun(id, runId)
      setReport({
        riskScore: historicalRun.riskScore,
        riskLevel: historicalRun.riskLevel,
        findings: historicalRun.findings || [],
      })
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Could not retrieve historical run')
    } finally {
      setHistoryLoading(false)
    }
  }

  const handleSubmitDecision = async () => {
    if (!id || !auditForm.decision || !auditForm.decidedBy.trim() || !auditForm.comment.trim()) return
    try {
      setAuditSubmitting(true)
      setAuditError(null)
      setAuditSuccess(false)
      const latestRunId = historyRuns.length > 0 ? historyRuns[0].analysisRunId : undefined
      await riskApi.createAuditDecision(id, {
        decision: auditForm.decision as AuditDecisionType,
        comment: auditForm.comment.trim(),
        decidedBy: auditForm.decidedBy.trim(),
        analysisRunId: latestRunId,
      })
      setAuditForm({ decision: '', comment: '', decidedBy: '' })
      setAuditSuccess(true)
      setTimeout(() => setAuditSuccess(false), 4000)
      await loadAuditData(id)
    } catch (err) {
      setAuditError(err instanceof Error ? err.message : 'Failed to record audit decision')
    } finally {
      setAuditSubmitting(false)
    }
  }

  // ── Derived values ────────────────────────────────────────────────────────────

  const findings: RiskFinding[] = useMemo(() => report?.findings || [], [report])
  const calculatedTotal = useMemo(() => findings.reduce((acc, f) => acc + (f.score || 0), 0), [findings])

  const latestAnalyzedAt = useMemo(() => {
    if (selectedRunId) {
      const sel = historyRuns.find((r) => r.analysisRunId === selectedRunId)
      return sel ? sel.analyzedAt : null
    }
    return historyRuns.length > 0 ? historyRuns[0].analyzedAt : null
  }, [historyRuns, selectedRunId])

  const isFormValid =
    !!auditForm.decision &&
    auditForm.decidedBy.trim().length > 0 &&
    auditForm.comment.trim().length > 0

  // ── Loading state ─────────────────────────────────────────────────────────────

  if (loading) {
    return (
      <div className="space-y-6 max-w-7xl mx-auto pb-12">
        <div className="flex items-center gap-3">
          <Skeleton className="h-8 w-8 rounded-lg" />
          <div className="space-y-2">
            <Skeleton className="h-5 w-48" />
            <Skeleton className="h-4 w-72" />
          </div>
        </div>
        <Skeleton className="h-40 w-full rounded-xl" />
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
          <div className="lg:col-span-8 space-y-4">
            <Skeleton className="h-6 w-40" />
            <Skeleton className="h-28 w-full rounded-xl" />
            <Skeleton className="h-28 w-full rounded-xl" />
          </div>
          <div className="lg:col-span-4 space-y-4">
            <Skeleton className="h-56 w-full rounded-xl" />
          </div>
        </div>
      </div>
    )
  }

  // ── Error state ───────────────────────────────────────────────────────────────

  if (error || !report || !transaction) {
    return (
      <div className="min-h-[500px] flex items-center justify-center p-6">
        <div className="p-8 max-w-md w-full rounded-2xl border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-[#14161b] shadow-sm text-center">
          <div className="w-12 h-12 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-500 flex items-center justify-center mx-auto mb-4">
            <ShieldAlert className="w-6 h-6" />
          </div>
          <h2 className="text-base font-bold text-zinc-900 dark:text-zinc-100">
            {error?.includes('not found') ? 'Transaction Not Found' : 'Unable to Load Investigation'}
          </h2>
          <p className="text-xs text-zinc-500 dark:text-zinc-400 mt-2 leading-relaxed">
            {error || `Transaction record for ${id} could not be retrieved.`}
          </p>
          <div className="flex items-center justify-center gap-3 mt-6">
            <button
              onClick={() => navigate('/transactions')}
              className="px-4 py-2 rounded-lg text-xs font-medium border border-zinc-200 dark:border-zinc-800 text-zinc-700 dark:text-zinc-300 hover:bg-zinc-50 dark:hover:bg-zinc-800 transition-colors"
            >
              Back to Transactions
            </button>
            <button
              onClick={loadInvestigation}
              className="inline-flex items-center gap-2 px-4 py-2 rounded-lg text-xs font-medium bg-zinc-900 dark:bg-zinc-100 text-white dark:text-zinc-900 hover:bg-zinc-800 dark:hover:bg-zinc-200 transition-colors"
            >
              <RotateCcw className="w-3.5 h-3.5" />
              Retry
            </button>
          </div>
        </div>
      </div>
    )
  }

  // ── Main render ───────────────────────────────────────────────────────────────

  return (
    <div className="space-y-6 max-w-7xl mx-auto pb-16">

      {/* TOP BAR */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-zinc-200/80 dark:border-zinc-800/80 pb-4">
        <div className="flex items-center gap-2 min-w-0">
          <button
            onClick={() => navigate('/transactions')}
            aria-label="Back to Transactions"
            className="group flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-900/60 text-xs font-medium text-zinc-600 dark:text-zinc-300 hover:text-zinc-900 dark:hover:text-zinc-100 hover:bg-zinc-50 dark:hover:bg-zinc-800/80 transition-all shadow-2xs shrink-0"
          >
            <ArrowLeft className="w-3.5 h-3.5 transition-transform group-hover:-translate-x-0.5" />
            Transactions
          </button>
          <span className="text-zinc-300 dark:text-zinc-700">/</span>
          <span className="text-xs text-zinc-400 dark:text-zinc-500">Investigation</span>
          <span className="text-zinc-300 dark:text-zinc-700">/</span>
          <div className="flex items-center gap-1 min-w-0">
            <span className="font-mono text-xs font-bold text-zinc-800 dark:text-zinc-200 truncate">
              {transaction.id}
            </span>
            <button
              onClick={handleCopyId}
              aria-label="Copy transaction ID to clipboard"
              className="p-1 rounded text-zinc-400 hover:text-zinc-700 dark:hover:text-zinc-200 hover:bg-zinc-100 dark:hover:bg-zinc-800 transition-colors shrink-0"
            >
              {copied ? (
                <span className="inline-flex items-center gap-1 text-[10px] font-mono text-emerald-600 dark:text-emerald-400 font-semibold">
                  <Check className="w-3 h-3" /> Copied
                </span>
              ) : (
                <Copy className="w-3 h-3" />
              )}
            </button>
          </div>
        </div>

        <div className="flex items-center gap-2 shrink-0">
          {selectedRunId && (
            <button
              onClick={() => handleSelectHistoricalRun(selectedRunId)}
              className="px-3 py-1.5 rounded-lg text-xs font-medium border border-amber-500/30 bg-amber-500/10 text-amber-700 dark:text-amber-400 hover:bg-amber-500/20 transition-colors"
            >
              Return to Current
            </button>
          )}
          <button
            onClick={handleRunAnalysis}
            disabled={analyzing}
            aria-label="Run and persist risk analysis"
            className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-lg text-xs font-medium bg-zinc-900 dark:bg-zinc-100 text-white dark:text-zinc-900 hover:bg-zinc-800 dark:hover:bg-zinc-200 transition-colors shadow-2xs disabled:opacity-50"
          >
            <Play className={`w-3.5 h-3.5 ${analyzing ? 'animate-pulse' : ''}`} />
            {analyzing ? 'Running Analysis…' : 'Run & Persist Analysis'}
          </button>
        </div>
      </div>

      {/* HISTORICAL RUN BANNER */}
      <AnimatePresence>
        {selectedRunId && (
          <motion.div
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: 'auto' }}
            exit={{ opacity: 0, height: 0 }}
            className="overflow-hidden"
          >
            <div className="flex items-center justify-between px-4 py-2.5 rounded-lg border border-amber-400/30 bg-amber-50/60 dark:bg-amber-500/5 text-amber-700 dark:text-amber-400">
              <div className="flex items-center gap-2 text-xs font-medium min-w-0">
                <History className="w-3.5 h-3.5 shrink-0" />
                <span>
                  Viewing <span className="font-bold font-mono">Historical Run #{selectedRunId}</span> — score, findings, and composition reflect this specific run, not the current analysis.
                </span>
              </div>
              <button
                onClick={() => handleSelectHistoricalRun(selectedRunId)}
                className="text-[11px] font-semibold underline underline-offset-2 ml-4 shrink-0 hover:opacity-70 transition-opacity"
              >
                View Current
              </button>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* TRANSACTION IDENTITY */}
      <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-3">
        <div className="min-w-0">
          <div className="flex items-center gap-2.5 flex-wrap">
            <h1 className="font-mono text-xl font-bold tracking-tight text-zinc-900 dark:text-zinc-100">
              {transaction.id}
            </h1>
            <RiskBadge severity={report.riskLevel} className="text-xs px-2.5 py-0.5" />
          </div>
          <p className="text-xs text-zinc-500 dark:text-zinc-400 mt-1 flex items-center gap-2 flex-wrap">
            <span className="font-semibold text-zinc-700 dark:text-zinc-300">{transaction.vendor}</span>
            <span className="text-zinc-300 dark:text-zinc-600">·</span>
            <span>{transaction.category}</span>
            <span className="text-zinc-300 dark:text-zinc-600">·</span>
            <span className="font-mono">{transaction.employee}</span>
          </p>
        </div>
        <div className="text-left sm:text-right shrink-0">
          <span className="text-[10px] uppercase font-bold tracking-wider text-zinc-400 dark:text-zinc-500 block">
            Transaction Value
          </span>
          <span className="font-mono text-lg font-bold text-zinc-900 dark:text-zinc-100">
            {formatCurrency(transaction.amount)}
          </span>
          <span className="block text-[10px] text-zinc-400 mt-0.5">
            {formatDate(transaction.transactionTime)}
          </span>
        </div>
      </div>

      {/* ═══════════════════════════════════════════════════════════════════════ */}
      {/* A. SYSTEM RISK ASSESSMENT                                               */}
      {/* ═══════════════════════════════════════════════════════════════════════ */}
      <section aria-label="System Risk Assessment">
        <div className="rounded-xl border border-zinc-200/90 dark:border-zinc-800/90 bg-white dark:bg-[#14161b] shadow-xs overflow-hidden">
          <div className="px-5 py-2.5 border-b border-zinc-100 dark:border-zinc-800/80 flex items-center justify-between bg-zinc-50/60 dark:bg-zinc-900/40">
            <div className="flex items-center gap-2">
              <ShieldAlert className="w-3.5 h-3.5 text-zinc-500 dark:text-zinc-400" />
              <span className="text-[10px] font-bold uppercase tracking-widest text-zinc-500 dark:text-zinc-400">
                System Risk Assessment
              </span>
            </div>
            <span className="text-[10px] font-mono text-zinc-400 dark:text-zinc-500">
              Generated by AUREX Risk Engine
            </span>
          </div>

          <div className="p-5 sm:p-6">
            <div className="flex flex-col md:flex-row items-center md:items-start gap-6 md:gap-10">
              {/* Gauge */}
              <div className="flex flex-col items-center gap-2 shrink-0">
                <RiskScoreGauge score={report.riskScore} size="xl" />
                <div className="text-center">
                  <RiskBadge severity={report.riskLevel} className="text-xs px-2.5 py-0.5" />
                  <p className="text-[10px] text-zinc-400 dark:text-zinc-500 mt-1.5">
                    System-generated score
                  </p>
                </div>
              </div>

              {/* Narrative + score breakdown */}
              <div className="flex-1 min-w-0 space-y-4 w-full">
                <div>
                  <h2 className="text-base font-bold text-zinc-900 dark:text-zinc-100 tracking-tight">
                    {report.riskScore >= 80
                      ? 'Critical Policy & Anomaly Flags'
                      : report.riskScore >= 60
                      ? 'High Risk Pattern Identified'
                      : report.riskScore >= 30
                      ? 'Moderate Exposure Detected'
                      : 'No Significant Risk Detected'}
                  </h2>
                  <p className="text-xs text-zinc-500 dark:text-zinc-400 mt-0.5 leading-relaxed">
                    Deterministic evaluation across 6 independent audit policies.{' '}
                    {findings.length > 0
                      ? `${findings.length} rule${findings.length > 1 ? 's' : ''} triggered — ${calculatedTotal} risk points accumulated.`
                      : 'No policy violations detected.'}
                  </p>
                  {latestAnalyzedAt && (
                    <p className="text-[11px] text-zinc-400 font-mono flex items-center gap-1.5 mt-2">
                      <Calendar className="w-3 h-3" />
                      {selectedRunId ? `Run #${selectedRunId} analyzed` : 'Last analyzed'}:{' '}
                      <span className="text-zinc-600 dark:text-zinc-300">{formatDate(latestAnalyzedAt)}</span>
                    </p>
                  )}
                </div>

                {findings.length > 0 && (
                  <div className="space-y-2 pt-3 border-t border-zinc-100 dark:border-zinc-800/60">
                    <div className="flex items-center justify-between">
                      <span className="text-[10px] uppercase font-bold text-zinc-400 tracking-wider">
                        Score Composition
                      </span>
                      <span className="font-mono text-xs font-bold text-zinc-700 dark:text-zinc-300">
                        {report.riskScore} / 100
                      </span>
                    </div>
                    {findings.map((f, i) => {
                      const pct = Math.min(100, Math.round((f.score / 100) * 100))
                      return (
                        <div key={`${f.type}-bar-${i}`} className="space-y-1">
                          <div className="flex items-center justify-between text-[11px]">
                            <span className="text-zinc-600 dark:text-zinc-400 truncate max-w-[70%]">
                              {f.type.replace(/_/g, ' ')}
                            </span>
                            <span className="font-mono font-bold text-zinc-800 dark:text-zinc-200 shrink-0 ml-2">
                              +{f.score}
                            </span>
                          </div>
                          <div className="w-full h-1.5 rounded-full bg-zinc-100 dark:bg-zinc-800 overflow-hidden">
                            <motion.div
                              className="h-full rounded-full bg-zinc-600 dark:bg-zinc-400"
                              initial={{ width: 0 }}
                              animate={{ width: `${pct}%` }}
                              transition={{ duration: 0.5, delay: i * 0.06, ease: 'easeOut' }}
                            />
                          </div>
                        </div>
                      )
                    })}
                  </div>
                )}
              </div>

              {/* Side metrics */}
              <div className="w-full md:w-44 grid grid-cols-2 md:grid-cols-1 gap-2 shrink-0">
                <div className="p-3 rounded-lg bg-zinc-50/80 dark:bg-zinc-900/50 border border-zinc-200/60 dark:border-zinc-800/60">
                  <span className="text-[9px] uppercase font-bold text-zinc-400 block tracking-wider">Rule Violations</span>
                  <span className="font-mono text-lg font-bold text-zinc-900 dark:text-zinc-100">{findings.length}</span>
                </div>
                <div className="p-3 rounded-lg bg-zinc-50/80 dark:bg-zinc-900/50 border border-zinc-200/60 dark:border-zinc-800/60">
                  <span className="text-[9px] uppercase font-bold text-zinc-400 block tracking-wider">Total Runs</span>
                  <span className="font-mono text-lg font-bold text-zinc-900 dark:text-zinc-100">{historyRuns.length}</span>
                </div>
                <div className="col-span-2 md:col-span-1 p-3 rounded-lg bg-zinc-50/80 dark:bg-zinc-900/50 border border-zinc-200/60 dark:border-zinc-800/60">
                  <span className="text-[9px] uppercase font-bold text-zinc-400 block tracking-wider">Active Run</span>
                  <span className="font-mono text-xs font-medium text-zinc-700 dark:text-zinc-300">
                    {selectedRunId
                      ? `Run #${selectedRunId}`
                      : historyRuns.length > 0
                      ? `Run #${historyRuns[0].analysisRunId} (latest)`
                      : 'Live preview'}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* ── MAIN TWO-COLUMN WORKSPACE ─────────────────────────────────────────── */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">

        {/* LEFT: Findings + Transaction Profile */}
        <div className="lg:col-span-8 space-y-6">

          {/* B. WHY WAS THIS FLAGGED? */}
          <section aria-label="Risk Findings">
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <ShieldAlert className="w-4 h-4 text-amber-500" />
                  <h3 className="text-xs font-bold uppercase tracking-wider text-zinc-700 dark:text-zinc-300">
                    Why Was This Flagged?
                  </h3>
                  <span className="px-2 py-0.5 rounded-full text-[10px] font-mono font-semibold bg-zinc-100 dark:bg-zinc-800 text-zinc-600 dark:text-zinc-400 border border-zinc-200 dark:border-zinc-700">
                    {findings.length}
                  </span>
                </div>
                <span className="text-[11px] text-zinc-400 font-mono hidden sm:block">
                  Deterministic Engine
                </span>
              </div>

              {historyLoading ? (
                <div className="space-y-2">
                  <Skeleton className="h-24 w-full rounded-xl" />
                  <Skeleton className="h-24 w-full rounded-xl" />
                </div>
              ) : findings.length > 0 ? (
                <div className="space-y-3">
                  <AnimatePresence mode="popLayout">
                    {findings.map((finding, idx) => (
                      <FindingCard
                        key={`${finding.type}-${idx}-${selectedRunId ?? 'live'}`}
                        finding={finding}
                        index={idx}
                      />
                    ))}
                  </AnimatePresence>
                </div>
              ) : (
                <div className="p-8 rounded-xl border border-dashed border-emerald-300 dark:border-emerald-800/50 bg-emerald-50/20 dark:bg-emerald-950/10 text-center">
                  <CheckCircle2 className="w-8 h-8 text-emerald-500 mx-auto mb-2.5" />
                  <h4 className="text-sm font-semibold text-zinc-900 dark:text-zinc-100">
                    No Policy Violations
                  </h4>
                  <p className="text-xs text-zinc-500 dark:text-zinc-400 mt-1 max-w-sm mx-auto">
                    This transaction passed all 6 configured rule evaluations with 0 risk points.
                  </p>
                </div>
              )}
            </div>
          </section>

          {/* TRANSACTION PROFILE */}
          <section aria-label="Transaction Profile">
            <div className="rounded-xl border border-zinc-200/90 dark:border-zinc-800/90 bg-white dark:bg-[#14161b] shadow-2xs overflow-hidden">
              <div className="px-5 py-3 border-b border-zinc-100 dark:border-zinc-800/80 flex items-center justify-between">
                <h3 className="text-xs font-bold uppercase tracking-wider text-zinc-600 dark:text-zinc-400">
                  Transaction Profile
                </h3>
                <span className="text-[10px] font-mono text-zinc-400">METADATA</span>
              </div>
              <div className="divide-y divide-zinc-100 dark:divide-zinc-800/60 text-xs">
                {[
                  { icon: Banknote, label: 'ID', value: <span className="font-mono font-bold">{transaction.id}</span> },
                  { icon: Building2, label: 'Vendor', value: transaction.vendor },
                  { icon: User, label: 'Employee', value: <span className="font-mono">{transaction.employee}</span> },
                  { icon: Layers, label: 'Category', value: transaction.category },
                  { icon: Banknote, label: 'Amount', value: <span className="font-mono font-bold text-sm">{formatCurrency(transaction.amount)}</span> },
                  { icon: Calendar, label: 'Timestamp', value: formatDate(transaction.transactionTime) },
                ].map(({ icon: Icon, label, value }) => (
                  <div key={label} className="flex items-center justify-between px-5 py-2.5">
                    <span className="flex items-center gap-1.5 text-zinc-400 dark:text-zinc-500">
                      <Icon className="w-3.5 h-3.5" />
                      {label}
                    </span>
                    <span className="text-zinc-800 dark:text-zinc-200 text-right max-w-[200px] truncate">
                      {value}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          </section>
        </div>

        {/* RIGHT: Analysis History */}
        <div className="lg:col-span-4">
          <section aria-label="Analysis History">
            <div className="rounded-xl border border-zinc-200/90 dark:border-zinc-800/90 bg-white dark:bg-[#14161b] shadow-2xs overflow-hidden">
              <div className="px-5 py-3 border-b border-zinc-100 dark:border-zinc-800/80 flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <History className="w-3.5 h-3.5 text-zinc-500" />
                  <h3 className="text-xs font-bold uppercase tracking-wider text-zinc-600 dark:text-zinc-400">
                    Analysis History
                  </h3>
                  <span className="px-1.5 rounded text-[10px] font-mono font-medium bg-zinc-100 dark:bg-zinc-800 text-zinc-500">
                    {historyRuns.length}
                  </span>
                </div>
                {selectedRunId && (
                  <button
                    onClick={() => handleSelectHistoricalRun(selectedRunId)}
                    className="text-[10px] text-zinc-500 hover:text-zinc-900 dark:hover:text-zinc-100 underline transition-colors"
                  >
                    View Current
                  </button>
                )}
              </div>

              <div className="p-3">
                {historyRuns.length > 0 ? (
                  <div className="space-y-1.5 max-h-72 overflow-y-auto">
                    {historyRuns.map((run, index) => {
                      const isLatest = index === 0
                      const isSelected = selectedRunId === run.analysisRunId
                      return (
                        <button
                          key={run.analysisRunId}
                          onClick={() => handleSelectHistoricalRun(run.analysisRunId)}
                          aria-pressed={isSelected}
                          aria-label={`View analysis run ${run.analysisRunId}${isLatest ? ' (latest)' : ''}`}
                          className={`w-full text-left p-2.5 rounded-lg border text-xs transition-all flex items-center justify-between ${
                            isSelected
                              ? 'bg-zinc-100 dark:bg-zinc-800/90 border-zinc-400 dark:border-zinc-600 shadow-2xs'
                              : 'bg-zinc-50/60 dark:bg-zinc-900/30 border-zinc-200/70 dark:border-zinc-800/70 hover:bg-zinc-100/70 dark:hover:bg-zinc-800/50 hover:border-zinc-300 dark:hover:border-zinc-700'
                          }`}
                        >
                          <div className="space-y-0.5 min-w-0">
                            <div className="flex items-center gap-1.5">
                              <span className="font-mono font-semibold text-zinc-900 dark:text-zinc-100">
                                Run #{run.analysisRunId}
                              </span>
                              {isLatest && !isSelected && (
                                <span className="px-1 rounded text-[8px] font-mono font-bold bg-blue-500/10 text-blue-600 dark:text-blue-400 border border-blue-500/20">
                                  CURRENT
                                </span>
                              )}
                              {isSelected && (
                                <span className="px-1 rounded text-[8px] font-mono font-bold bg-amber-500/10 text-amber-600 dark:text-amber-400 border border-amber-500/20">
                                  VIEWING
                                </span>
                              )}
                            </div>
                            <p className="text-[10px] text-zinc-400 truncate">
                              {formatRelativeTime(run.analyzedAt)}
                            </p>
                          </div>
                          <div className="text-right flex items-center gap-2 shrink-0">
                            <div>
                              <span className="font-mono font-bold text-zinc-900 dark:text-zinc-100 block">
                                {run.riskScore}
                              </span>
                              <span className="text-[9px] text-zinc-400 font-mono">
                                {run.findings?.length ?? 0} findings
                              </span>
                            </div>
                            <RiskBadge severity={run.riskLevel} showDot={false} className="text-[9px] px-1.5 py-0.5" />
                          </div>
                        </button>
                      )
                    })}
                  </div>
                ) : (
                  <div className="py-6 text-center text-xs text-zinc-400 dark:text-zinc-500 space-y-2">
                    <p>No persisted runs yet.</p>
                    <button
                      onClick={handleRunAnalysis}
                      disabled={analyzing}
                      className="text-[11px] text-zinc-600 dark:text-zinc-300 underline hover:text-zinc-900 dark:hover:text-zinc-100 transition-colors"
                    >
                      Persist first run now
                    </button>
                  </div>
                )}
              </div>
            </div>
          </section>
        </div>
      </div>

      {/* ═══════════════════════════════════════════════════════════════════════ */}
      {/* C. EVIDENCE & RELATIONSHIPS                                             */}
      {/* ═══════════════════════════════════════════════════════════════════════ */}
      <section aria-label="Evidence and Relationships">
        <div className="rounded-xl border border-zinc-200/90 dark:border-zinc-800/90 bg-white dark:bg-[#14161b] shadow-2xs overflow-hidden">
          <div className="px-5 py-3 border-b border-zinc-100 dark:border-zinc-800/80 flex items-center justify-between bg-zinc-50/60 dark:bg-zinc-900/40">
            <div className="flex items-center gap-2">
              <GitBranch className="w-3.5 h-3.5 text-zinc-500" />
              <h2 className="text-xs font-bold uppercase tracking-wider text-zinc-600 dark:text-zinc-400">
                Evidence &amp; Relationships
              </h2>
            </div>
            <span className="text-[10px] font-mono text-zinc-400 hidden sm:block">
              Cross-entity risk context
            </span>
          </div>

          <div className="p-5">
            {evidenceLoading ? (
              <div className="space-y-4">
                <div className="grid grid-cols-3 gap-3">
                  <Skeleton className="h-14 rounded-lg" />
                  <Skeleton className="h-14 rounded-lg" />
                  <Skeleton className="h-14 rounded-lg" />
                </div>
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                  <Skeleton className="h-36 rounded-lg" />
                  <Skeleton className="h-36 rounded-lg" />
                  <Skeleton className="h-36 rounded-lg" />
                </div>
              </div>
            ) : evidenceError ? (
              <div className="p-4 rounded-lg border border-red-200 dark:border-red-900/40 bg-red-50/50 dark:bg-red-950/20 text-xs flex items-center justify-between">
                <div className="flex items-center gap-2 text-red-600 dark:text-red-400">
                  <AlertTriangle className="w-4 h-4 shrink-0" />
                  Failed to load evidence context: {evidenceError}
                </div>
                <button
                  onClick={() => id && loadEvidence(id)}
                  className="px-3 py-1 text-xs font-semibold rounded-lg bg-red-100 dark:bg-red-900/60 text-red-700 dark:text-red-300 hover:bg-red-200 ml-4 shrink-0"
                >
                  Retry
                </button>
              </div>
            ) : evidenceContext ? (
              <div className="space-y-5">
                {/* Summary strip */}
                <div className="grid grid-cols-3 gap-3">
                  <div className="p-3 rounded-lg bg-zinc-50 dark:bg-zinc-900/50 border border-zinc-200/80 dark:border-zinc-800/80">
                    <span className="text-[9px] uppercase font-bold text-zinc-400 block tracking-wider">Related Txns</span>
                    <span className="font-mono text-base font-bold text-zinc-900 dark:text-zinc-100">
                      {evidenceContext.evidenceSummary.relatedTransactionCount}
                    </span>
                  </div>
                  <div className={`p-3 rounded-lg border ${
                    evidenceContext.evidenceSummary.relatedFlaggedTransactionCount > 0
                      ? 'bg-amber-50/60 border-amber-400/25 dark:bg-amber-500/5 dark:border-amber-500/20'
                      : 'bg-zinc-50 dark:bg-zinc-900/50 border-zinc-200/80 dark:border-zinc-800/80'
                  }`}>
                    <span className={`text-[9px] uppercase font-bold block tracking-wider ${
                      evidenceContext.evidenceSummary.relatedFlaggedTransactionCount > 0
                        ? 'text-amber-600 dark:text-amber-400'
                        : 'text-zinc-400'
                    }`}>Flagged Related</span>
                    <span className={`font-mono text-base font-bold ${
                      evidenceContext.evidenceSummary.relatedFlaggedTransactionCount > 0
                        ? 'text-amber-700 dark:text-amber-300'
                        : 'text-zinc-900 dark:text-zinc-100'
                    }`}>
                      {evidenceContext.evidenceSummary.relatedFlaggedTransactionCount}
                    </span>
                  </div>
                  <div className="p-3 rounded-lg bg-zinc-50 dark:bg-zinc-900/50 border border-zinc-200/80 dark:border-zinc-800/80">
                    <span className="text-[9px] uppercase font-bold text-zinc-400 block tracking-wider">Related Amount</span>
                    <span className="font-mono text-sm font-bold text-zinc-900 dark:text-zinc-100">
                      {formatCurrency(evidenceContext.evidenceSummary.relatedAmount)}
                    </span>
                  </div>
                </div>

                {/* Relationship tables */}
                <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
                  <RelationshipGroupCard
                    title="Vendor"
                    icon={<Building2 className="w-3.5 h-3.5 text-blue-500" />}
                    group={evidenceContext.vendor}
                    onNavigate={(txnId) => navigate(`/transactions/${txnId}`)}
                  />
                  <RelationshipGroupCard
                    title="Employee"
                    icon={<User className="w-3.5 h-3.5 text-indigo-500" />}
                    group={evidenceContext.employee}
                    onNavigate={(txnId) => navigate(`/transactions/${txnId}`)}
                  />
                  <RelationshipGroupCard
                    title="Category"
                    icon={<Layers className="w-3.5 h-3.5 text-purple-500" />}
                    group={evidenceContext.category}
                    onNavigate={(txnId) => navigate(`/transactions/${txnId}`)}
                  />
                </div>
              </div>
            ) : (
              <div className="py-8 text-center text-xs text-zinc-400 dark:text-zinc-500">
                No evidence context available.
              </div>
            )}
          </div>
        </div>
      </section>

      {/* ═══════════════════════════════════════════════════════════════════════ */}
      {/* D. AUDITOR DECISION — visually distinct from System Risk Assessment      */}
      {/* ═══════════════════════════════════════════════════════════════════════ */}
      <section aria-label="Auditor Decision">
        <div className="rounded-xl border border-zinc-200/90 dark:border-zinc-800/90 bg-white dark:bg-[#14161b] shadow-2xs overflow-hidden">
          {/* Header — clearly different section label */}
          <div className="px-5 py-2.5 border-b border-zinc-100 dark:border-zinc-800/80 flex items-center justify-between bg-zinc-50/60 dark:bg-zinc-900/40">
            <div className="flex items-center gap-2">
              <Scale className="w-3.5 h-3.5 text-indigo-500 dark:text-indigo-400" />
              <span className="text-[10px] font-bold uppercase tracking-widest text-zinc-500 dark:text-zinc-400">
                Auditor Decision
              </span>
            </div>
            {latestDecision && (
              <AuditDecisionBadge decision={latestDecision.decision} />
            )}
          </div>

          {/* Domain-critical disclaimer */}
          <div className="px-5 py-2.5 border-b border-zinc-100 dark:border-zinc-800/60 bg-indigo-50/40 dark:bg-indigo-500/5">
            <p className="text-[11px] text-indigo-700 dark:text-indigo-400 flex items-start gap-1.5 leading-relaxed">
              <Info className="w-3.5 h-3.5 shrink-0 mt-px" aria-hidden="true" />
              Auditor decisions record the investigation outcome and <strong>do not change</strong> the
              system-generated risk score. The engine score remains{' '}
              <strong>{report.riskScore} {report.riskLevel}</strong> regardless of this decision.
            </p>
          </div>

          <div className="p-5 grid grid-cols-1 lg:grid-cols-2 gap-6">

            {/* LEFT: Current decision + form */}
            <div className="space-y-5">

              {/* Current decision summary */}
              {latestDecision ? (
                <div className="p-4 rounded-lg border border-zinc-200/80 dark:border-zinc-800/80 bg-zinc-50/60 dark:bg-zinc-900/30 space-y-2.5">
                  <div className="flex items-center justify-between">
                    <span className="text-[10px] uppercase font-bold text-zinc-400 tracking-wider">
                      Current Decision
                    </span>
                    <AuditDecisionBadge decision={latestDecision.decision} />
                  </div>
                  {latestDecision.comment && (
                    <p className="text-xs text-zinc-600 dark:text-zinc-400 italic leading-relaxed border-l-2 border-zinc-200 dark:border-zinc-700 pl-3">
                      "{latestDecision.comment}"
                    </p>
                  )}
                  <div className="flex items-center gap-1.5 text-[10px] text-zinc-400">
                    <User className="w-3 h-3" />
                    <span className="font-medium text-zinc-600 dark:text-zinc-400">{latestDecision.decidedBy}</span>
                    <span className="text-zinc-300 dark:text-zinc-700">·</span>
                    <Clock className="w-3 h-3" />
                    <span>{formatRelativeTime(latestDecision.decidedAt)}</span>
                  </div>
                </div>
              ) : !auditLoading ? (
                <div className="p-4 rounded-lg border border-dashed border-zinc-200 dark:border-zinc-800 text-center">
                  <Scale className="w-6 h-6 text-zinc-300 dark:text-zinc-600 mx-auto mb-1.5" />
                  <p className="text-xs text-zinc-400 dark:text-zinc-500">No decision recorded yet.</p>
                </div>
              ) : (
                <Skeleton className="h-20 w-full rounded-lg" />
              )}

              {/* Form */}
              <div className="space-y-4">
                <h3 className="text-xs font-bold uppercase tracking-wider text-zinc-600 dark:text-zinc-400 flex items-center gap-2">
                  <Send className="w-3.5 h-3.5" />
                  Record New Decision
                </h3>

                {/* Success */}
                <AnimatePresence>
                  {auditSuccess && (
                    <motion.div
                      initial={{ opacity: 0, y: -4 }}
                      animate={{ opacity: 1, y: 0 }}
                      exit={{ opacity: 0, y: -4 }}
                      className="flex items-center gap-2 p-3 rounded-lg border border-emerald-300 dark:border-emerald-700/50 bg-emerald-50 dark:bg-emerald-950/20 text-xs text-emerald-700 dark:text-emerald-400"
                    >
                      <CheckCircle2 className="w-4 h-4 shrink-0" />
                      Decision recorded. The audit trail has been updated.
                    </motion.div>
                  )}
                </AnimatePresence>

                {/* Error */}
                {auditError && (
                  <div className="p-3 rounded-lg border border-red-200 dark:border-red-900/40 bg-red-50 dark:bg-red-950/20 text-xs text-red-600 dark:text-red-400 flex items-center gap-2">
                    <AlertTriangle className="w-3.5 h-3.5 shrink-0" />
                    {auditError}
                  </div>
                )}

                <div className="space-y-3">
                  {/* Decision buttons */}
                  <fieldset>
                    <legend className="block text-[10px] uppercase font-bold text-zinc-400 mb-2 tracking-wider">
                      Decision <span className="text-red-400" aria-label="required">*</span>
                    </legend>
                    <div className="grid grid-cols-2 gap-2">
                      {(['CONFIRMED_RISK', 'FALSE_POSITIVE', 'REQUIRES_INVESTIGATION', 'ESCALATED'] as AuditDecisionType[]).map((dtype) => (
                        <button
                          key={dtype}
                          type="button"
                          aria-pressed={auditForm.decision === dtype}
                          aria-label={decisionLabel(dtype)}
                          onClick={() => setAuditForm(f => ({ ...f, decision: f.decision === dtype ? '' : dtype }))}
                          className={`px-3 py-2.5 rounded-lg text-left border transition-all leading-tight ${
                            auditForm.decision === dtype
                              ? decisionActiveStyle(dtype)
                              : 'border-zinc-200 dark:border-zinc-800 bg-zinc-50 dark:bg-zinc-900/40 text-zinc-600 dark:text-zinc-400 hover:border-zinc-300 dark:hover:border-zinc-700'
                          }`}
                        >
                          <span className="block text-[11px] font-bold">{decisionLabel(dtype)}</span>
                          <span className="block text-[10px] font-normal opacity-70 mt-0.5">
                            {decisionDescription(dtype)}
                          </span>
                        </button>
                      ))}
                    </div>
                  </fieldset>

                  {/* Auditor name */}
                  <div>
                    <label
                      htmlFor="audit-decided-by"
                      className="block text-[10px] uppercase font-bold text-zinc-400 mb-1.5 tracking-wider"
                    >
                      Auditor Name <span className="text-red-400" aria-label="required">*</span>
                    </label>
                    <input
                      id="audit-decided-by"
                      type="text"
                      value={auditForm.decidedBy}
                      onChange={e => setAuditForm(f => ({ ...f, decidedBy: e.target.value }))}
                      placeholder="Your name or identifier"
                      maxLength={100}
                      className="w-full px-3 py-2 rounded-lg border border-zinc-200 dark:border-zinc-800 bg-zinc-50 dark:bg-zinc-900/60 text-xs text-zinc-800 dark:text-zinc-200 placeholder-zinc-400 focus:outline-none focus:ring-1 focus:ring-indigo-400 dark:focus:ring-indigo-600 transition-colors"
                    />
                  </div>

                  {/* Comment */}
                  <div>
                    <label
                      htmlFor="audit-comment"
                      className="block text-[10px] uppercase font-bold text-zinc-400 mb-1.5 tracking-wider"
                    >
                      Comment <span className="text-red-400" aria-label="required">*</span>
                    </label>
                    <textarea
                      id="audit-comment"
                      value={auditForm.comment}
                      onChange={e => setAuditForm(f => ({ ...f, comment: e.target.value }))}
                      placeholder="Describe your audit rationale and findings…"
                      rows={3}
                      maxLength={2000}
                      className="w-full px-3 py-2 rounded-lg border border-zinc-200 dark:border-zinc-800 bg-zinc-50 dark:bg-zinc-900/60 text-xs text-zinc-800 dark:text-zinc-200 placeholder-zinc-400 focus:outline-none focus:ring-1 focus:ring-indigo-400 dark:focus:ring-indigo-600 transition-colors resize-none"
                    />
                    <p className="text-[10px] text-zinc-400 mt-1 text-right">
                      {auditForm.comment.length} / 2000
                    </p>
                  </div>

                  <button
                    type="button"
                    disabled={auditSubmitting || !isFormValid}
                    onClick={handleSubmitDecision}
                    aria-label="Submit audit decision"
                    className="w-full inline-flex items-center justify-center gap-2 px-4 py-2.5 rounded-lg text-xs font-semibold bg-indigo-600 hover:bg-indigo-700 active:bg-indigo-800 disabled:opacity-40 disabled:cursor-not-allowed text-white transition-colors shadow-sm"
                  >
                    {auditSubmitting ? (
                      <>
                        <motion.span
                          className="inline-block w-3.5 h-3.5 border-2 border-white/30 border-t-white rounded-full"
                          animate={{ rotate: 360 }}
                          transition={{ duration: 0.7, repeat: Infinity, ease: 'linear' }}
                        />
                        Submitting…
                      </>
                    ) : (
                      <>
                        <Send className="w-3.5 h-3.5" />
                        Submit Decision
                      </>
                    )}
                  </button>

                  {!isFormValid && !auditSubmitting && (
                    <p className="text-[10px] text-zinc-400 text-center">
                      Decision, auditor name, and comment are all required.
                    </p>
                  )}
                </div>
              </div>
            </div>

            {/* RIGHT: Audit Trail */}
            <div className="space-y-4">
              <div className="flex items-center gap-2">
                <Clock className="w-3.5 h-3.5 text-zinc-500" />
                <h3 className="text-xs font-bold uppercase tracking-wider text-zinc-600 dark:text-zinc-400">
                  Audit Trail
                </h3>
                <span className="px-1.5 rounded text-[10px] font-mono bg-zinc-100 dark:bg-zinc-800 text-zinc-500">
                  {auditEvents.length}
                </span>
                <div className="flex items-center gap-1 ml-auto">
                  <Lock className="w-3 h-3 text-zinc-300 dark:text-zinc-600" aria-hidden="true" />
                  <span className="text-[10px] text-zinc-300 dark:text-zinc-600">Immutable</span>
                </div>
              </div>

              {auditLoading ? (
                <div className="space-y-2">
                  <Skeleton className="h-16 w-full rounded-lg" />
                  <Skeleton className="h-16 w-full rounded-lg" />
                </div>
              ) : auditEvents.length === 0 ? (
                <div className="py-10 text-center rounded-xl border border-dashed border-zinc-200 dark:border-zinc-800 bg-zinc-50/50 dark:bg-zinc-900/20">
                  <FileText className="w-7 h-7 text-zinc-300 dark:text-zinc-600 mx-auto mb-2" />
                  <p className="text-xs text-zinc-400 dark:text-zinc-500">No audit events yet.</p>
                  <p className="text-[11px] text-zinc-300 dark:text-zinc-600 mt-0.5">
                    Submit a decision to begin the immutable trail.
                  </p>
                </div>
              ) : (
                <div className="relative">
                  <div className="absolute left-3.5 top-3 bottom-3 w-px bg-zinc-200 dark:bg-zinc-800 pointer-events-none" aria-hidden="true" />
                  <div className="space-y-0 max-h-96 overflow-y-auto pl-0.5 pr-1">
                    {auditEvents.map((event) => (
                      <div key={event.id} className="flex gap-3 pb-4">
                        <div className="shrink-0 w-7 h-7 rounded-full bg-white dark:bg-[#14161b] border-2 border-indigo-300 dark:border-indigo-700 flex items-center justify-center z-10">
                          <span className="w-2 h-2 rounded-full bg-indigo-400 dark:bg-indigo-500" />
                        </div>
                        <div className="flex-1 pt-0.5 min-w-0">
                          <div className="flex items-start justify-between gap-2">
                            <span className="font-mono font-bold text-[10px] uppercase tracking-wider text-indigo-600 dark:text-indigo-400">
                              {event.eventType.replace(/_/g, ' ')}
                            </span>
                            <span className="text-[10px] font-mono text-zinc-400 shrink-0">
                              {formatRelativeTime(event.createdAt)}
                            </span>
                          </div>
                          <p className="text-[11px] text-zinc-600 dark:text-zinc-400 mt-0.5 leading-relaxed">
                            {event.eventDetails}
                          </p>
                          <div className="flex items-center gap-1 mt-1 text-[10px] text-zinc-400">
                            <User className="w-3 h-3" />
                            <span>{event.actor}</span>
                            <span className="text-zinc-300 dark:text-zinc-700 mx-1">·</span>
                            <span className="font-mono">{formatDate(event.createdAt)}</span>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      </section>
    </div>
  )
}

// ── RelationshipGroupCard ─────────────────────────────────────────────────────

interface RelationshipGroupCardProps {
  title: string
  icon: React.ReactNode
  group: RelatedGroup
  onNavigate: (txnId: string) => void
}

const RelationshipGroupCard: React.FC<RelationshipGroupCardProps> = ({
  title,
  icon,
  group,
  onNavigate,
}) => (
  <div className="rounded-lg border border-zinc-200/80 dark:border-zinc-800/80 bg-zinc-50/50 dark:bg-zinc-900/30 overflow-hidden">
    {/* Header */}
    <div className="px-3 py-2.5 border-b border-zinc-200/60 dark:border-zinc-800/60 bg-white dark:bg-[#14161b] flex items-center justify-between">
      <div className="flex items-center gap-1.5">
        {icon}
        <span className="text-xs font-bold text-zinc-700 dark:text-zinc-300">{title}</span>
      </div>
      <span className="text-[10px] font-semibold text-zinc-500 dark:text-zinc-400 truncate max-w-[100px]">
        {group.name || '—'}
      </span>
    </div>

    {/* Stat row */}
    <div className="grid grid-cols-4 divide-x divide-zinc-200/60 dark:divide-zinc-800/60 border-b border-zinc-200/60 dark:border-zinc-800/60">
      {[
        { label: 'Txns', value: String(group.relatedTransactionCount), warn: false },
        { label: 'Flagged', value: String(group.flaggedTransactionCount), warn: group.flaggedTransactionCount > 0 },
        { label: 'Peak', value: group.highestRiskScore > 0 ? String(group.highestRiskScore) : '—', warn: group.highestRiskScore >= 75 },
        { label: 'Amt', value: formatCurrency(group.totalAmount).replace('INR ', ''), warn: false, small: true },
      ].map(({ label, value, warn, small }) => (
        <div key={label} className="p-2 text-center">
          <span className="text-[8px] uppercase font-bold text-zinc-400 block tracking-wider">{label}</span>
          <span className={`font-mono font-bold block leading-tight mt-0.5 ${small ? 'text-[10px]' : 'text-xs'} ${warn ? 'text-amber-600 dark:text-amber-400' : 'text-zinc-800 dark:text-zinc-200'}`}>
            {value}
          </span>
        </div>
      ))}
    </div>

    {/* Transaction list */}
    {group.transactions && group.transactions.length > 0 ? (
      <div className="max-h-36 overflow-y-auto">
        <table className="w-full text-left text-[11px]">
          <thead className="sticky top-0 bg-zinc-100 dark:bg-zinc-800/90 text-zinc-500 text-[9px] uppercase font-bold">
            <tr>
              <th className="px-2.5 py-1.5">ID</th>
              <th className="px-2.5 py-1.5 text-right">Amount</th>
              <th className="px-2.5 py-1.5 text-center">Score</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-zinc-100 dark:divide-zinc-800/60 font-mono">
            {group.transactions.map((tx) => (
              <tr
                key={tx.transactionId}
                onClick={() => onNavigate(tx.transactionId)}
                className="hover:bg-zinc-100/70 dark:hover:bg-zinc-800/40 cursor-pointer transition-colors"
              >
                <td className="px-2.5 py-1.5 font-semibold text-blue-600 dark:text-blue-400">
                  <span className="flex items-center gap-0.5">
                    {tx.transactionId}
                    <ExternalLink className="w-2.5 h-2.5 opacity-50" />
                  </span>
                </td>
                <td className="px-2.5 py-1.5 text-right text-zinc-600 dark:text-zinc-400 text-[10px]">
                  {formatCurrency(tx.amount)}
                </td>
                <td className="px-2.5 py-1.5 text-center">
                  {tx.riskScore !== null ? (
                    <span className="font-bold text-zinc-800 dark:text-zinc-200">{tx.riskScore}</span>
                  ) : (
                    <span className="text-zinc-300 dark:text-zinc-600 italic text-[9px]">—</span>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    ) : (
      <div className="py-4 text-center text-[11px] text-zinc-400 dark:text-zinc-500">
        No related transactions.
      </div>
    )}
  </div>
)
