import React, { useEffect, useState, useMemo } from 'react'
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
} from 'lucide-react'
import { riskApi } from '../api/risk'
import { transactionsApi } from '../api/transactions'
import type {
  RiskReport,
  Transaction,
  RiskAnalysisHistoryItem,
  RiskFinding,
} from '../types'
import { RiskBadge } from '../components/common/RiskBadge'
import { RiskScoreGauge } from '../components/common/RiskScoreGauge'
import { FindingCard } from '../components/risk/FindingCard'
import { Skeleton } from '../components/common/Skeleton'
import { formatCurrency, formatDate } from '../lib/utils'

export const TransactionInvestigation: React.FC = () => {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()

  const [transaction, setTransaction] = useState<Transaction | null>(null)
  const [allTransactions, setAllTransactions] = useState<Transaction[]>([])
  const [report, setReport] = useState<RiskReport | null>(null)
  const [historyRuns, setHistoryRuns] = useState<RiskAnalysisHistoryItem[]>([])
  const [selectedRunId, setSelectedRunId] = useState<number | null>(null)

  const [loading, setLoading] = useState(true)
  const [analyzing, setAnalyzing] = useState(false)
  const [copied, setCopied] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [historyLoading, setHistoryLoading] = useState(false)

  const loadInvestigation = async () => {
    if (!id) return
    try {
      setLoading(true)
      setError(null)
      setSelectedRunId(null)

      // Fetch transaction risk report, history, and all transactions for accurate metadata/context
      const [reportData, historyData, transactionsList] = await Promise.all([
        riskApi.analyzeTransaction(id),
        riskApi.getTransactionHistory(id).catch(() => ({ transactionId: id, analysisRuns: [] })),
        transactionsApi.getAll().catch(() => []),
      ])

      setReport(reportData)
      setHistoryRuns(historyData.analysisRuns || [])
      setAllTransactions(transactionsList)

      const matchingTx = transactionsList.find((t) => t.id === id)
      if (matchingTx) {
        setTransaction(matchingTx)
      } else {
        // If not found in the transaction registry, mark error
        setError(`Transaction ${id} not found in repository`)
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : `Could not load transaction ${id}`)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadInvestigation()
  }, [id])

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

      // Refresh history runs
      const historyData = await riskApi.getTransactionHistory(id)
      setHistoryRuns(historyData.analysisRuns || [])
      setSelectedRunId(null)
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
        // Deselect historical run, return to live report
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
      alert(err instanceof Error ? err.message : 'Could not retrieve historical run details')
    } finally {
      setHistoryLoading(false)
    }
  }

  // Calculate score breakdown sum from actual findings
  const findings: RiskFinding[] = useMemo(() => {
    return report?.findings || []
  }, [report])

  const calculatedTotal = useMemo(() => {
    return findings.reduce((acc, f) => acc + (f.score || 0), 0)
  }, [findings])

  // Related transactions context from existing transaction list
  const relatedVendorTxCount = useMemo(() => {
    if (!transaction || !allTransactions.length) return 0
    return allTransactions.filter((t) => t.vendor === transaction.vendor && t.id !== transaction.id).length
  }, [transaction, allTransactions])

  const relatedEmployeeTxCount = useMemo(() => {
    if (!transaction || !allTransactions.length) return 0
    return allTransactions.filter((t) => t.employee === transaction.employee && t.id !== transaction.id).length
  }, [transaction, allTransactions])

  const relatedCategoryTxCount = useMemo(() => {
    if (!transaction || !allTransactions.length) return 0
    return allTransactions.filter((t) => t.category === transaction.category && t.id !== transaction.id).length
  }, [transaction, allTransactions])

  // Get most recent analysis timestamp
  const latestAnalyzedAt = useMemo(() => {
    if (selectedRunId) {
      const selected = historyRuns.find((r) => r.analysisRunId === selectedRunId)
      return selected ? selected.analyzedAt : null
    }
    if (historyRuns.length > 0) {
      return historyRuns[0].analyzedAt
    }
    return null
  }, [historyRuns, selectedRunId])

  // SKELETON LOADING STATE
  if (loading) {
    return (
      <div className="space-y-6 max-w-7xl mx-auto pb-12">
        {/* Navigation & Header Skeleton */}
        <div className="flex items-center gap-3">
          <Skeleton className="h-9 w-9 rounded-lg" />
          <div className="space-y-2">
            <Skeleton className="h-6 w-48" />
            <Skeleton className="h-4 w-72" />
          </div>
        </div>

        {/* Hero Card Skeleton */}
        <div className="p-6 rounded-xl border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-[#14161b] flex flex-col md:flex-row gap-6 items-center">
          <Skeleton className="w-28 h-28 rounded-full" />
          <div className="flex-1 space-y-3 w-full">
            <Skeleton className="h-6 w-36" />
            <Skeleton className="h-8 w-64" />
            <Skeleton className="h-4 w-96 max-w-full" />
          </div>
          <div className="w-full md:w-64 space-y-2">
            <Skeleton className="h-14 w-full rounded-lg" />
            <Skeleton className="h-14 w-full rounded-lg" />
          </div>
        </div>

        {/* Two Column Grid Skeleton */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
          <div className="lg:col-span-8 space-y-6">
            <div className="space-y-3">
              <Skeleton className="h-6 w-48" />
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
                <Skeleton className="h-32 w-full rounded-xl" />
                <Skeleton className="h-32 w-full rounded-xl" />
              </div>
            </div>
            <div className="space-y-3">
              <Skeleton className="h-6 w-56" />
              <Skeleton className="h-44 w-full rounded-xl" />
            </div>
          </div>
          <div className="lg:col-span-4 space-y-6">
            <Skeleton className="h-64 w-full rounded-xl" />
            <Skeleton className="h-64 w-full rounded-xl" />
          </div>
        </div>
      </div>
    )
  }

  // ERROR STATE
  if (error || !report || !transaction) {
    return (
      <div className="min-h-[500px] flex items-center justify-center p-6">
        <div className="p-8 max-w-md w-full rounded-2xl border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-[#14161b] shadow-sm text-center">
          <div className="w-12 h-12 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-500 flex items-center justify-center mx-auto mb-4">
            <ShieldAlert className="w-6 h-6" />
          </div>
          <h2 className="text-base font-bold text-zinc-900 dark:text-zinc-100">
            {error?.includes('not found') ? 'Transaction not found' : 'Unable to Load Investigation'}
          </h2>
          <p className="text-xs text-zinc-500 dark:text-zinc-400 mt-2 leading-relaxed">
            {error || `Transaction record for ${id} could not be retrieved from the database.`}
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

  return (
    <div className="space-y-6 max-w-7xl mx-auto pb-12">
      {/* 1. BREADCRUMB / BACK NAVIGATION & TOP BAR */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-zinc-200/80 dark:border-zinc-800/80 pb-4">
        <div className="flex items-center gap-3">
          <button
            onClick={() => navigate('/transactions')}
            className="group flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-900/60 text-xs font-medium text-zinc-600 dark:text-zinc-300 hover:text-zinc-900 dark:hover:text-zinc-100 hover:bg-zinc-50 dark:hover:bg-zinc-800/80 transition-all shadow-2xs"
          >
            <ArrowLeft className="w-3.5 h-3.5 transition-transform group-hover:-translate-x-0.5" />
            <span>Transactions</span>
          </button>

          <span className="text-zinc-300 dark:text-zinc-700">/</span>

          <span className="font-mono text-xs font-semibold text-zinc-400 dark:text-zinc-500">
            Investigation
          </span>

          <span className="text-zinc-300 dark:text-zinc-700">/</span>

          <div className="flex items-center gap-1.5">
            <span className="font-mono text-xs font-bold text-zinc-800 dark:text-zinc-200">
              {transaction.id}
            </span>
            <button
              onClick={handleCopyId}
              title="Copy Transaction ID"
              className="p-1 rounded text-zinc-400 hover:text-zinc-700 dark:hover:text-zinc-200 hover:bg-zinc-100 dark:hover:bg-zinc-800 transition-colors"
            >
              {copied ? (
                <span className="inline-flex items-center gap-1 text-[10px] font-mono text-emerald-600 dark:text-emerald-400 font-semibold px-1 py-0.2 bg-emerald-500/10 rounded">
                  <Check className="w-3 h-3" /> Copied
                </span>
              ) : (
                <Copy className="w-3 h-3" />
              )}
            </button>
          </div>
        </div>

        {/* Action Controls */}
        <div className="flex items-center gap-2.5">
          {selectedRunId && (
            <button
              onClick={() => handleSelectHistoricalRun(selectedRunId)}
              className="px-3 py-1.5 rounded-lg text-xs font-medium border border-amber-500/30 bg-amber-500/10 text-amber-700 dark:text-amber-400 hover:bg-amber-500/20 transition-colors"
            >
              Return to Live Analysis
            </button>
          )}

          <button
            onClick={handleRunAnalysis}
            disabled={analyzing}
            className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-lg text-xs font-medium bg-zinc-900 dark:bg-zinc-100 text-white dark:text-zinc-900 hover:bg-zinc-800 dark:hover:bg-zinc-200 transition-colors shadow-2xs disabled:opacity-50"
          >
            <Play className={`w-3.5 h-3.5 ${analyzing ? 'animate-spin' : ''}`} />
            {analyzing ? 'Evaluating Engine...' : 'Run & Persist Analysis'}
          </button>
        </div>
      </div>

      {/* 2. TRANSACTION IDENTITY HEADER */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-3">
            <h1 className="font-mono text-2xl font-bold tracking-tight text-zinc-900 dark:text-zinc-100">
              {transaction.id}
            </h1>
            <RiskBadge severity={report.riskLevel} className="text-xs px-2.5 py-0.5" />
            {selectedRunId && (
              <span className="px-2 py-0.5 rounded text-[10px] font-mono font-medium bg-amber-500/10 text-amber-600 dark:text-amber-400 border border-amber-500/20">
                Viewing Run #{selectedRunId}
              </span>
            )}
          </div>
          <p className="text-xs text-zinc-500 dark:text-zinc-400 mt-1 flex items-center gap-2 flex-wrap">
            <span className="font-medium text-zinc-800 dark:text-zinc-200">{transaction.vendor}</span>
            <span>•</span>
            <span>{transaction.category}</span>
            <span>•</span>
            <span className="font-mono">{transaction.employee}</span>
          </p>
        </div>

        <div className="text-left sm:text-right">
          <span className="text-[10px] uppercase font-bold tracking-wider text-zinc-400 dark:text-zinc-500 block">
            Transaction Value
          </span>
          <span className="font-mono text-xl font-bold text-zinc-900 dark:text-zinc-100">
            {formatCurrency(transaction.amount)}
          </span>
        </div>
      </div>

      {/* 3. RISK SCORE HERO */}
      <div className="relative overflow-hidden rounded-2xl border border-zinc-200/90 dark:border-zinc-800/90 bg-white dark:bg-[#14161b] p-6 sm:p-7 shadow-xs">
        <div className="flex flex-col md:flex-row items-center justify-between gap-8">
          <div className="flex flex-col sm:flex-row items-center gap-6 sm:gap-8 text-center sm:text-left">
            <div className="relative shrink-0">
              <RiskScoreGauge score={report.riskScore} size="xl" />
            </div>

            <div className="space-y-1.5">
              <div className="flex items-center justify-center sm:justify-start gap-2">
                <RiskBadge severity={report.riskLevel} className="text-xs px-3 py-1 font-semibold" />
                <span className="text-xs text-zinc-400 font-mono">
                  {report.riskScore} / 100 Risk Score
                </span>
              </div>

              <h2 className="text-xl sm:text-2xl font-bold text-zinc-900 dark:text-zinc-100 tracking-tight">
                {report.riskScore >= 80
                  ? 'Critical Policy & Anomaly Flags'
                  : report.riskScore >= 60
                  ? 'High Risk Pattern Identified'
                  : report.riskScore >= 30
                  ? 'Moderate Exposure Detected'
                  : 'Low Risk Normal Transaction'}
              </h2>

              <p className="text-xs text-zinc-500 dark:text-zinc-400 max-w-xl leading-relaxed">
                Deterministic audit evaluation computed across 6 independent audit policies.
                {findings.length > 0
                  ? ` Triggered ${findings.length} findings totaling ${calculatedTotal} risk points.`
                  : ' No policy violations or velocity anomalies detected.'}
              </p>

              {latestAnalyzedAt && (
                <div className="pt-2 text-[11px] text-zinc-400 font-mono flex items-center justify-center sm:justify-start gap-1.5">
                  <Calendar className="w-3.5 h-3.5" />
                  <span>Last Analyzed:</span>
                  <span className="text-zinc-600 dark:text-zinc-300 font-medium">
                    {formatDate(latestAnalyzedAt)}
                  </span>
                </div>
              )}
            </div>
          </div>

          {/* Quick Metrics Widget */}
          <div className="w-full md:w-auto grid grid-cols-2 sm:grid-cols-2 gap-3 min-w-[240px]">
            <div className="p-3.5 rounded-xl bg-zinc-50/80 dark:bg-zinc-900/50 border border-zinc-200/60 dark:border-zinc-800/60">
              <span className="text-[10px] uppercase font-semibold text-zinc-400 dark:text-zinc-500 block">
                Rule Violations
              </span>
              <span className="font-mono text-lg font-bold text-zinc-900 dark:text-zinc-100">
                {findings.length}
              </span>
            </div>

            <div className="p-3.5 rounded-xl bg-zinc-50/80 dark:bg-zinc-900/50 border border-zinc-200/60 dark:border-zinc-800/60">
              <span className="text-[10px] uppercase font-semibold text-zinc-400 dark:text-zinc-500 block">
                Audit Status
              </span>
              <span className="text-xs font-semibold text-zinc-800 dark:text-zinc-200 flex items-center gap-1.5 mt-1">
                {findings.length > 0 ? (
                  <span className="text-amber-600 dark:text-amber-400">Review Required</span>
                ) : (
                  <span className="text-emerald-600 dark:text-emerald-400">Compliant</span>
                )}
              </span>
            </div>

            <div className="col-span-2 p-3.5 rounded-xl bg-zinc-50/80 dark:bg-zinc-900/50 border border-zinc-200/60 dark:border-zinc-800/60 flex items-center justify-between">
              <div>
                <span className="text-[10px] uppercase font-semibold text-zinc-400 dark:text-zinc-500 block">
                  Analysis Run
                </span>
                <span className="font-mono text-xs font-medium text-zinc-700 dark:text-zinc-300">
                  {selectedRunId ? `Run #${selectedRunId}` : historyRuns.length > 0 ? `Latest (Run #${historyRuns[0].analysisRunId})` : 'Live Preview'}
                </span>
              </div>
              <span className="text-[10px] font-mono text-zinc-400">
                {historyRuns.length} Total Runs
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* MAIN TWO-COLUMN WORKSPACE */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        {/* LEFT PANE (8 Columns): Why Flagged & Contribution Breakdown */}
        <div className="lg:col-span-8 space-y-6">
          {/* 4. WHY WAS THIS FLAGGED? */}
          <div className="space-y-3.5">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <ShieldAlert className="w-4 h-4 text-amber-500" />
                <h3 className="text-xs font-bold uppercase tracking-wider text-zinc-700 dark:text-zinc-300">
                  Why Was This Flagged?
                </h3>
                <span className="px-2 py-0.5 rounded-full text-[10px] font-mono font-medium bg-zinc-100 dark:bg-zinc-800 text-zinc-600 dark:text-zinc-400 border border-zinc-200 dark:border-zinc-700">
                  {findings.length}
                </span>
              </div>
              <span className="text-[11px] font-mono text-zinc-400">Deterministic Engine Findings</span>
            </div>

            {findings.length > 0 ? (
              <div className="grid grid-cols-1 gap-3">
                <AnimatePresence mode="popLayout">
                  {findings.map((finding, idx) => (
                    <FindingCard
                      key={`${finding.type}-${idx}-${selectedRunId || 'live'}`}
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
                  No Policy Violations Found
                </h4>
                <p className="text-xs text-zinc-500 dark:text-zinc-400 mt-1 max-w-sm mx-auto">
                  This transaction passed all 6 configured rule evaluations with 0 risk points.
                </p>
              </div>
            )}
          </div>

          {/* 5. RISK CONTRIBUTION BREAKDOWN */}
          <div className="rounded-xl border border-zinc-200/90 dark:border-zinc-800/90 bg-white dark:bg-[#14161b] p-5 shadow-2xs space-y-4">
            <div className="flex items-center justify-between border-b border-zinc-100 dark:border-zinc-800/80 pb-3">
              <div>
                <h3 className="text-xs font-bold uppercase tracking-wider text-zinc-700 dark:text-zinc-300">
                  Risk Score Composition
                </h3>
                <p className="text-[11px] text-zinc-400 mt-0.5">
                  Direct summation of policy violation points contributing to the final score
                </p>
              </div>
              <div className="text-right">
                <span className="font-mono text-xs font-semibold text-zinc-400">Total Score:</span>
                <span className="font-mono text-sm font-bold text-zinc-900 dark:text-zinc-100 ml-1.5">
                  {report.riskScore}
                </span>
              </div>
            </div>

            {findings.length > 0 ? (
              <div className="space-y-3 pt-1">
                {findings.map((f, i) => {
                  const pct = Math.min(100, Math.round(((f.score || 0) / 100) * 100))
                  return (
                    <div key={`${f.type}-breakdown-${i}`} className="space-y-1.5">
                      <div className="flex items-center justify-between text-xs">
                        <div className="flex items-center gap-2">
                          <span className="w-1.5 h-1.5 rounded-full bg-zinc-400 dark:bg-zinc-600" />
                          <span className="font-medium text-zinc-800 dark:text-zinc-200">
                            {f.type.replace(/_/g, ' ')}
                          </span>
                        </div>
                        <span className="font-mono font-bold text-zinc-900 dark:text-zinc-100">
                          +{f.score}
                        </span>
                      </div>

                      {/* Clean horizontal contribution bar */}
                      <div className="w-full h-1.5 rounded-full bg-zinc-100 dark:bg-zinc-800/80 overflow-hidden">
                        <motion.div
                          className="h-full rounded-full bg-zinc-700 dark:bg-zinc-300"
                          initial={{ width: 0 }}
                          animate={{ width: `${pct}%` }}
                          transition={{ duration: 0.5, delay: i * 0.08 }}
                        />
                      </div>
                    </div>
                  )
                })}

                <div className="pt-3 border-t border-zinc-100 dark:border-zinc-800/80 flex items-center justify-between text-xs font-mono">
                  <span className="font-semibold text-zinc-500 uppercase tracking-wider text-[10px]">
                    Total Calculated Score
                  </span>
                  <div className="flex items-center gap-2">
                    <span className="font-bold text-sm text-zinc-900 dark:text-zinc-100">
                      {report.riskScore}
                    </span>
                    <span className="text-[10px] text-zinc-400">/ 100</span>
                  </div>
                </div>
              </div>
            ) : (
              <div className="py-4 text-center text-xs text-zinc-400 dark:text-zinc-500">
                No active score contributions. Score is 0 / 100.
              </div>
            )}
          </div>
        </div>

        {/* RIGHT PANE (4 Columns): Profile & Analysis History */}
        <div className="lg:col-span-4 space-y-6">
          {/* 6. TRANSACTION PROFILE */}
          <div className="rounded-xl border border-zinc-200/90 dark:border-zinc-800/90 bg-white dark:bg-[#14161b] p-5 shadow-2xs space-y-4">
            <h3 className="text-xs font-bold uppercase tracking-wider text-zinc-700 dark:text-zinc-300 border-b border-zinc-100 dark:border-zinc-800/80 pb-2.5 flex items-center justify-between">
              <span>Transaction Profile</span>
              <span className="font-mono text-[10px] text-zinc-400">METADATA</span>
            </h3>

            <div className="space-y-3 text-xs">
              <div className="flex items-center justify-between py-1.5 border-b border-zinc-100 dark:border-zinc-800/50">
                <span className="text-zinc-400 dark:text-zinc-500 flex items-center gap-1.5">
                  <Banknote className="w-3.5 h-3.5" />
                  ID
                </span>
                <span className="font-mono font-bold text-zinc-900 dark:text-zinc-100">
                  {transaction.id}
                </span>
              </div>

              <div className="flex items-center justify-between py-1.5 border-b border-zinc-100 dark:border-zinc-800/50">
                <span className="text-zinc-400 dark:text-zinc-500 flex items-center gap-1.5">
                  <Building2 className="w-3.5 h-3.5" />
                  Vendor
                </span>
                <span className="font-medium text-zinc-900 dark:text-zinc-100 truncate max-w-[180px] text-right">
                  {transaction.vendor}
                </span>
              </div>

              <div className="flex items-center justify-between py-1.5 border-b border-zinc-100 dark:border-zinc-800/50">
                <span className="text-zinc-400 dark:text-zinc-500 flex items-center gap-1.5">
                  <User className="w-3.5 h-3.5" />
                  Employee
                </span>
                <span className="font-mono font-medium text-zinc-900 dark:text-zinc-100">
                  {transaction.employee}
                </span>
              </div>

              <div className="flex items-center justify-between py-1.5 border-b border-zinc-100 dark:border-zinc-800/50">
                <span className="text-zinc-400 dark:text-zinc-500 flex items-center gap-1.5">
                  <Layers className="w-3.5 h-3.5" />
                  Category
                </span>
                <span className="font-medium text-zinc-900 dark:text-zinc-100">
                  {transaction.category}
                </span>
              </div>

              <div className="flex items-center justify-between py-1.5 border-b border-zinc-100 dark:border-zinc-800/50">
                <span className="text-zinc-400 dark:text-zinc-500">Amount</span>
                <span className="font-mono font-bold text-zinc-900 dark:text-zinc-100 text-sm">
                  {formatCurrency(transaction.amount)}
                </span>
              </div>

              <div className="flex items-center justify-between py-1.5">
                <span className="text-zinc-400 dark:text-zinc-500 flex items-center gap-1.5">
                  <Calendar className="w-3.5 h-3.5" />
                  Timestamp
                </span>
                <span className="font-medium text-zinc-700 dark:text-zinc-300 text-right">
                  {formatDate(transaction.transactionTime)}
                </span>
              </div>
            </div>
          </div>

          {/* 7. RELATED TRANSACTION CONTEXT (Extension Point supported by existing dataset) */}
          <div className="rounded-xl border border-zinc-200/90 dark:border-zinc-800/90 bg-white dark:bg-[#14161b] p-5 shadow-2xs space-y-3">
            <h3 className="text-xs font-bold uppercase tracking-wider text-zinc-700 dark:text-zinc-300 border-b border-zinc-100 dark:border-zinc-800/80 pb-2.5 flex items-center justify-between">
              <span>Related Transaction Context</span>
              <span className="text-[10px] font-mono text-zinc-400">DATASET</span>
            </h3>

            <div className="grid grid-cols-3 gap-2 text-center">
              <div className="p-2.5 rounded-lg bg-zinc-50 dark:bg-zinc-900/60 border border-zinc-100 dark:border-zinc-800/60">
                <span className="text-[9px] uppercase font-semibold text-zinc-400 block truncate">
                  Same Vendor
                </span>
                <span className="font-mono text-sm font-bold text-zinc-900 dark:text-zinc-100">
                  {relatedVendorTxCount}
                </span>
              </div>

              <div className="p-2.5 rounded-lg bg-zinc-50 dark:bg-zinc-900/60 border border-zinc-100 dark:border-zinc-800/60">
                <span className="text-[9px] uppercase font-semibold text-zinc-400 block truncate">
                  Same Employee
                </span>
                <span className="font-mono text-sm font-bold text-zinc-900 dark:text-zinc-100">
                  {relatedEmployeeTxCount}
                </span>
              </div>

              <div className="p-2.5 rounded-lg bg-zinc-50 dark:bg-zinc-900/60 border border-zinc-100 dark:border-zinc-800/60">
                <span className="text-[9px] uppercase font-semibold text-zinc-400 block truncate">
                  Same Category
                </span>
                <span className="font-mono text-sm font-bold text-zinc-900 dark:text-zinc-100">
                  {relatedCategoryTxCount}
                </span>
              </div>
            </div>
            <p className="text-[10px] text-zinc-400 dark:text-zinc-500 leading-normal">
              Context calculated from the currently loaded transaction repository without external API calls.
            </p>
          </div>

          {/* 8. ANALYSIS HISTORY */}
          <div className="rounded-xl border border-zinc-200/90 dark:border-zinc-800/90 bg-white dark:bg-[#14161b] p-5 shadow-2xs space-y-3.5">
            <div className="flex items-center justify-between border-b border-zinc-100 dark:border-zinc-800/80 pb-2.5">
              <div className="flex items-center gap-2">
                <History className="w-3.5 h-3.5 text-zinc-500" />
                <h3 className="text-xs font-bold uppercase tracking-wider text-zinc-700 dark:text-zinc-300">
                  Analysis History
                </h3>
                <span className="px-1.5 py-0.2 rounded text-[10px] font-mono font-medium bg-zinc-100 dark:bg-zinc-800 text-zinc-500">
                  {historyRuns.length}
                </span>
              </div>

              {selectedRunId && (
                <button
                  onClick={() => handleSelectHistoricalRun(selectedRunId)}
                  className="text-[10px] text-zinc-500 hover:text-zinc-900 dark:hover:text-zinc-100 underline transition-colors"
                >
                  View Live
                </button>
              )}
            </div>

            {historyLoading ? (
              <div className="space-y-2 py-2">
                <Skeleton className="h-12 w-full rounded-lg" />
                <Skeleton className="h-12 w-full rounded-lg" />
              </div>
            ) : historyRuns.length > 0 ? (
              <div className="space-y-2 max-h-72 overflow-y-auto pr-1">
                {historyRuns.map((run, index) => {
                  const isLatest = index === 0
                  const isSelected = selectedRunId === run.analysisRunId

                  return (
                    <div
                      key={run.analysisRunId}
                      onClick={() => handleSelectHistoricalRun(run.analysisRunId)}
                      className={`group p-3 rounded-lg border text-xs cursor-pointer transition-all flex items-center justify-between ${
                        isSelected
                          ? 'bg-zinc-100 dark:bg-zinc-800/90 border-zinc-400 dark:border-zinc-600 shadow-2xs'
                          : 'bg-zinc-50/70 dark:bg-zinc-900/40 border-zinc-200/70 dark:border-zinc-800/70 hover:bg-zinc-100/70 dark:hover:bg-zinc-800/50 hover:border-zinc-300 dark:hover:border-zinc-700'
                      }`}
                    >
                      <div className="space-y-1">
                        <div className="flex items-center gap-2">
                          <span className="font-mono font-semibold text-zinc-900 dark:text-zinc-100">
                            Run #{run.analysisRunId}
                          </span>
                          {isLatest && !isSelected && (
                            <span className="px-1.5 py-0.2 rounded text-[9px] font-mono font-semibold bg-blue-500/10 text-blue-600 dark:text-blue-400 border border-blue-500/20">
                              LATEST
                            </span>
                          )}
                          {isSelected && (
                            <span className="px-1.5 py-0.2 rounded text-[9px] font-mono font-semibold bg-amber-500/10 text-amber-600 dark:text-amber-400 border border-amber-500/20">
                              ACTIVE
                            </span>
                          )}
                        </div>
                        <p className="text-[10px] text-zinc-400 dark:text-zinc-500">
                          {formatDate(run.analyzedAt)}
                        </p>
                      </div>

                      <div className="text-right flex items-center gap-2.5">
                        <div className="space-y-0.5">
                          <span className="font-mono font-bold text-zinc-900 dark:text-zinc-100 block">
                            {run.riskScore}
                          </span>
                          <span className="text-[10px] text-zinc-400 block font-mono">
                            {run.findings?.length || 0} findings
                          </span>
                        </div>
                        <RiskBadge
                          severity={run.riskLevel}
                          showDot={false}
                          className="text-[9px] px-1.5 py-0.5"
                        />
                      </div>
                    </div>
                  )
                })}
              </div>
            ) : (
              <div className="py-6 text-center text-xs text-zinc-400 dark:text-zinc-500">
                <p>No historical runs persisted yet.</p>
                <button
                  onClick={handleRunAnalysis}
                  disabled={analyzing}
                  className="mt-2 text-[11px] text-zinc-600 dark:text-zinc-300 underline hover:text-zinc-900 dark:hover:text-zinc-100"
                >
                  Persist first run now
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
