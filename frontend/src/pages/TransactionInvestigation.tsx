import React, { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  ArrowLeft,
  ShieldAlert,
  Play,
  History,
  CheckCircle2,
} from 'lucide-react'
import { riskApi } from '../api/risk'
import { transactionsApi } from '../api/transactions'
import type {
  RiskReport,
  Transaction,
  RiskAnalysisHistoryItem,
} from '../types'
import { RiskBadge } from '../components/common/RiskBadge'
import { RiskScoreGauge } from '../components/common/RiskScoreGauge'
import { FindingCard } from '../components/risk/FindingCard'
import { Skeleton } from '../components/common/Skeleton'
import { ErrorState } from '../components/common/ErrorState'
import { formatCurrency, formatDate } from '../lib/utils'

export const TransactionInvestigation: React.FC = () => {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()

  const [transaction, setTransaction] = useState<Transaction | null>(null)
  const [report, setReport] = useState<RiskReport | null>(null)
  const [historyRuns, setHistoryRuns] = useState<RiskAnalysisHistoryItem[]>([])
  const [selectedRunId, setSelectedRunId] = useState<number | null>(null)

  const [loading, setLoading] = useState(true)
  const [analyzing, setAnalyzing] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const loadInvestigation = async () => {
    if (!id) return
    try {
      setLoading(true)
      setError(null)

      // 1. Fetch risk report for transaction
      const [reportData, historyData, allTransactions] = await Promise.all([
        riskApi.analyzeTransaction(id),
        riskApi.getTransactionHistory(id).catch(() => ({ transactionId: id, analysisRuns: [] })),
        transactionsApi.getAll().catch(() => []),
      ])

      setReport(reportData)
      setHistoryRuns(historyData.analysisRuns || [])

      const matchingTx = allTransactions.find((t) => t.id === id)
      if (matchingTx) {
        setTransaction(matchingTx)
      } else {
        // Fallback transaction metadata from query / findings if not in top 100
        setTransaction({
          id,
          vendor: 'Investigated Entity',
          employee: 'Assigned User',
          amount: 0,
          transactionTime: new Date().toISOString(),
          category: 'Audit Case',
        })
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : `Transaction ${id} not found`)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadInvestigation()
  }, [id])

  const handleRunAnalysis = async () => {
    if (!id) return
    try {
      setAnalyzing(true)
      const persistedReport = await riskApi.analyzeAndPersist(id)
      setReport(persistedReport)

      // Refresh history
      const historyData = await riskApi.getTransactionHistory(id)
      setHistoryRuns(historyData.analysisRuns || [])
      setSelectedRunId(null)
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Analysis failed')
    } finally {
      setAnalyzing(false)
    }
  }

  const handleSelectHistoricalRun = async (runId: number) => {
    if (!id) return
    try {
      if (selectedRunId === runId) {
        // deselect and return to live report
        setSelectedRunId(null)
        const liveReport = await riskApi.analyzeTransaction(id)
        setReport(liveReport)
        return
      }

      setSelectedRunId(runId)
      const historicalRun = await riskApi.getHistoryRun(id, runId)
      setReport({
        riskScore: historicalRun.riskScore,
        riskLevel: historicalRun.riskLevel,
        findings: historicalRun.findings,
      })
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Could not load historical run')
    }
  }

  if (loading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-6 w-32" />
        <div className="p-8 rounded-xl border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 flex flex-col md:flex-row gap-6">
          <Skeleton className="w-32 h-32 rounded-full" />
          <div className="flex-1 space-y-3">
            <Skeleton className="h-8 w-48" />
            <Skeleton className="h-4 w-64" />
            <Skeleton className="h-4 w-40" />
          </div>
        </div>
      </div>
    )
  }

  if (error || !report) {
    return (
      <ErrorState
        title="Transaction Investigation Unavailable"
        message={error || `Could not find transaction record for ID ${id}`}
        showBack
        onRetry={loadInvestigation}
      />
    )
  }

  return (
    <div className="space-y-6">
      {/* Top Bar / Navigation */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <button
            onClick={() => navigate('/transactions')}
            className="p-1.5 rounded-lg border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-800 text-zinc-600 dark:text-zinc-300 hover:bg-zinc-50 dark:hover:bg-zinc-700 transition-colors"
            aria-label="Back to transactions"
          >
            <ArrowLeft className="w-4 h-4" />
          </button>
          <div>
            <div className="flex items-center gap-2">
              <span className="font-mono text-sm font-semibold text-zinc-500 dark:text-zinc-400">
                Transaction ID:
              </span>
              <h1 className="font-mono text-lg font-bold text-zinc-900 dark:text-zinc-100">
                {id}
              </h1>
              {selectedRunId && (
                <span className="px-2 py-0.5 rounded text-[10px] font-mono font-medium bg-amber-500/10 text-amber-600 dark:text-amber-400 border border-amber-500/20">
                  Viewing Run #{selectedRunId}
                </span>
              )}
            </div>
            <p className="text-xs text-zinc-500 dark:text-zinc-400 mt-0.5">
              Comprehensive rule evaluation and deterministic audit breakdown
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2.5">
          <button
            onClick={handleRunAnalysis}
            disabled={analyzing}
            className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-lg text-xs font-medium bg-zinc-900 dark:bg-zinc-100 text-white dark:text-zinc-900 hover:bg-zinc-800 dark:hover:bg-zinc-200 transition-colors shadow-2xs disabled:opacity-50"
          >
            <Play className={`w-3.5 h-3.5 ${analyzing ? 'animate-spin' : ''}`} />
            {analyzing ? 'Evaluating...' : 'Persist Analysis Run'}
          </button>
        </div>
      </div>

      {/* Main Score Banner */}
      <div className="p-6 rounded-xl border border-zinc-200/80 dark:border-zinc-800/80 bg-white dark:bg-[#14161b] shadow-2xs flex flex-col md:flex-row items-center justify-between gap-6">
        <div className="flex flex-col sm:flex-row items-center gap-6 text-center sm:text-left">
          <RiskScoreGauge score={report.riskScore} size="lg" />
          <div>
            <div className="flex items-center justify-center sm:justify-start gap-2 mb-1.5">
              <RiskBadge severity={report.riskLevel} className="text-xs px-3 py-1" />
              <span className="text-xs text-zinc-400 dark:text-zinc-500 font-mono">
                Score: {report.riskScore} / 100
              </span>
            </div>
            <h2 className="text-lg font-bold text-zinc-900 dark:text-zinc-100">
              {report.riskScore >= 80
                ? 'Critical Audit Risk Detected'
                : report.riskScore >= 60
                ? 'High Risk Transaction Warning'
                : report.riskScore >= 30
                ? 'Moderate Risk Flags Identified'
                : 'Standard Low Risk Profile'}
            </h2>
            <p className="text-xs text-zinc-500 dark:text-zinc-400 mt-1 max-w-lg">
              Evaluated across 6 independent audit rules including round amount anomalies, vendor
              concentration, and transaction velocity.
            </p>
          </div>
        </div>

        {/* Quick details summary */}
        {transaction && (
          <div className="w-full md:w-auto grid grid-cols-2 gap-3 p-3.5 rounded-lg bg-zinc-50 dark:bg-zinc-900/60 border border-zinc-200/60 dark:border-zinc-800/60 text-xs">
            <div>
              <span className="text-[10px] uppercase font-medium text-zinc-400">Vendor</span>
              <p className="font-semibold text-zinc-800 dark:text-zinc-200 truncate max-w-[120px]">
                {transaction.vendor}
              </p>
            </div>
            <div>
              <span className="text-[10px] uppercase font-medium text-zinc-400">Employee</span>
              <p className="font-semibold text-zinc-800 dark:text-zinc-200 truncate max-w-[120px]">
                {transaction.employee}
              </p>
            </div>
            <div>
              <span className="text-[10px] uppercase font-medium text-zinc-400">Amount</span>
              <p className="font-mono font-semibold text-zinc-900 dark:text-zinc-100">
                {transaction.amount ? formatCurrency(transaction.amount) : '—'}
              </p>
            </div>
            <div>
              <span className="text-[10px] uppercase font-medium text-zinc-400">Category</span>
              <p className="font-semibold text-zinc-800 dark:text-zinc-200 truncate max-w-[120px]">
                {transaction.category || 'General'}
              </p>
            </div>
          </div>
        )}
      </div>

      {/* Two Column Layout: Findings (Left) & Metadata / History (Right) */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left 2 Cols: Why was this flagged? */}
        <div className="lg:col-span-2 space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-bold tracking-wide uppercase text-zinc-500 dark:text-zinc-400 flex items-center gap-2">
              <ShieldAlert className="w-4 h-4 text-orange-500" />
              Why Was This Flagged? ({report.findings?.length || 0})
            </h3>
            <span className="text-xs text-zinc-400 font-mono">Deterministic Findings</span>
          </div>

          {report.findings && report.findings.length > 0 ? (
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
              {report.findings.map((finding, idx) => (
                <FindingCard key={`${finding.type}-${idx}`} finding={finding} index={idx} />
              ))}
            </div>
          ) : (
            <div className="p-8 rounded-xl border border-dashed border-emerald-300 dark:border-emerald-800/40 bg-emerald-50/30 dark:bg-emerald-950/10 text-center">
              <CheckCircle2 className="w-8 h-8 text-emerald-500 mx-auto mb-2" />
              <h4 className="text-sm font-semibold text-zinc-900 dark:text-zinc-100">
                Clean Audit Status
              </h4>
              <p className="text-xs text-zinc-500 dark:text-zinc-400 mt-1 max-w-sm mx-auto">
                No risk flags or policy anomalies were triggered for this transaction under current
                engine parameters.
              </p>
            </div>
          )}
        </div>

        {/* Right 1 Col: Transaction Details & Analysis History */}
        <div className="space-y-6">
          {/* Detailed Transaction Info */}
          {transaction && (
            <div className="p-4 rounded-xl border border-zinc-200/80 dark:border-zinc-800/80 bg-white dark:bg-[#14161b] shadow-2xs space-y-3">
              <h3 className="text-xs font-bold uppercase tracking-wider text-zinc-500 dark:text-zinc-400 border-b border-zinc-100 dark:border-zinc-800 pb-2">
                Transaction Details
              </h3>

              <div className="space-y-2 text-xs">
                <div className="flex justify-between py-1 border-b border-zinc-50 dark:border-zinc-800/40">
                  <span className="text-zinc-400">ID</span>
                  <span className="font-mono font-medium text-zinc-900 dark:text-zinc-100">
                    {transaction.id}
                  </span>
                </div>
                <div className="flex justify-between py-1 border-b border-zinc-50 dark:border-zinc-800/40">
                  <span className="text-zinc-400">Vendor</span>
                  <span className="font-medium text-zinc-900 dark:text-zinc-100 truncate max-w-[160px]">
                    {transaction.vendor}
                  </span>
                </div>
                <div className="flex justify-between py-1 border-b border-zinc-50 dark:border-zinc-800/40">
                  <span className="text-zinc-400">Employee</span>
                  <span className="font-medium text-zinc-900 dark:text-zinc-100">
                    {transaction.employee}
                  </span>
                </div>
                <div className="flex justify-between py-1 border-b border-zinc-50 dark:border-zinc-800/40">
                  <span className="text-zinc-400">Amount</span>
                  <span className="font-mono font-semibold text-zinc-900 dark:text-zinc-100">
                    {formatCurrency(transaction.amount)}
                  </span>
                </div>
                <div className="flex justify-between py-1 border-b border-zinc-50 dark:border-zinc-800/40">
                  <span className="text-zinc-400">Category</span>
                  <span className="font-medium text-zinc-900 dark:text-zinc-100">
                    {transaction.category || 'General'}
                  </span>
                </div>
                <div className="flex justify-between py-1">
                  <span className="text-zinc-400">Timestamp</span>
                  <span className="text-zinc-700 dark:text-zinc-300">
                    {formatDate(transaction.transactionTime)}
                  </span>
                </div>
              </div>
            </div>
          )}

          {/* Analysis History Runs */}
          <div className="p-4 rounded-xl border border-zinc-200/80 dark:border-zinc-800/80 bg-white dark:bg-[#14161b] shadow-2xs space-y-3">
            <div className="flex items-center justify-between border-b border-zinc-100 dark:border-zinc-800 pb-2">
              <h3 className="text-xs font-bold uppercase tracking-wider text-zinc-500 dark:text-zinc-400 flex items-center gap-1.5">
                <History className="w-3.5 h-3.5" />
                Historical Runs ({historyRuns.length})
              </h3>
              {selectedRunId && (
                <button
                  onClick={() => handleSelectHistoricalRun(selectedRunId)}
                  className="text-[10px] text-zinc-500 hover:text-zinc-900 dark:hover:text-zinc-100 underline"
                >
                  View Live
                </button>
              )}
            </div>

            {historyRuns.length > 0 ? (
              <div className="space-y-2 max-h-60 overflow-y-auto pr-1">
                {historyRuns.map((run) => (
                  <div
                    key={run.analysisRunId}
                    onClick={() => handleSelectHistoricalRun(run.analysisRunId)}
                    className={`p-2.5 rounded-lg border text-xs cursor-pointer transition-colors flex items-center justify-between ${
                      selectedRunId === run.analysisRunId
                        ? 'bg-zinc-100 dark:bg-zinc-800 border-zinc-400 dark:border-zinc-600'
                        : 'bg-zinc-50/60 dark:bg-zinc-900/40 border-zinc-200/60 dark:border-zinc-800/60 hover:bg-zinc-100/60 dark:hover:bg-zinc-800/40'
                    }`}
                  >
                    <div>
                      <div className="flex items-center gap-1.5">
                        <span className="font-mono font-semibold text-zinc-800 dark:text-zinc-200">
                          Run #{run.analysisRunId}
                        </span>
                        <RiskBadge severity={run.riskLevel} showDot={false} className="text-[9px] px-1.5 py-0" />
                      </div>
                      <p className="text-[10px] text-zinc-400 mt-0.5">
                        {formatDate(run.analyzedAt)}
                      </p>
                    </div>

                    <div className="text-right">
                      <span className="font-mono font-bold text-zinc-900 dark:text-zinc-100">
                        {run.riskScore}
                      </span>
                      <span className="text-[10px] text-zinc-400 block">
                        {run.findings?.length || 0} findings
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <p className="text-xs text-zinc-400 dark:text-zinc-500 py-3 text-center">
                No persisted historical analysis runs for this transaction yet. Click 'Persist Analysis Run' above.
              </p>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
