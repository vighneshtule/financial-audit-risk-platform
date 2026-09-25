import React, { useState, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  UploadCloud,
  FileText,
  CheckCircle2,
  AlertCircle,
  RefreshCw,
  Info,
  Play,
  Layers,
  LayoutDashboard,
  RotateCcw,
} from 'lucide-react'
import { formatCurrency } from '../lib/utils'
import { transactionsApi } from '../api/transactions'
import { riskApi } from '../api/risk'
import type { AnalysisResult } from '../types'

interface ParsedRow {
  id: string
  vendor: string
  employee: string
  amount: number
  transactionTime: string
  category: string
}

export const ImportPage: React.FC = () => {
  const navigate = useNavigate()
  const fileInputRef = useRef<HTMLInputElement>(null)

  const [file, setFile] = useState<File | null>(null)
  const [parsedRows, setParsedRows] = useState<ParsedRow[]>([])
  const [isDragging, setIsDragging] = useState(false)
  const [isImporting, setIsImporting] = useState(false)
  const [importSuccess, setImportSuccess] = useState(false)
  const [importError, setImportError] = useState<string | null>(null)
  const [importedCount, setImportedCount] = useState<number>(0)

  // Analysis state
  const [isAnalyzing, setIsAnalyzing] = useState(false)
  const [analysisError, setAnalysisError] = useState<string | null>(null)
  const [analysisResult, setAnalysisResult] = useState<AnalysisResult | null>(null)

  const parseCsvText = (text: string) => {
    const lines = text
      .split(/\r?\n/)
      .map((l) => l.trim())
      .filter((l) => l.length > 0)

    if (lines.length <= 1) {
      throw new Error('CSV file is empty or does not contain data rows')
    }

    const headers = lines[0].split(',').map((h) => h.trim().toLowerCase().replace(/['"]/g, ''))
    const idIdx = headers.findIndex((h) => h.includes('id'))
    const vendorIdx = headers.findIndex((h) => h.includes('vendor'))
    const empIdx = headers.findIndex((h) => h.includes('employee') || h.includes('emp'))
    const amtIdx = headers.findIndex((h) => h.includes('amount'))
    const timeIdx = headers.findIndex((h) => h.includes('time') || h.includes('date'))
    const catIdx = headers.findIndex((h) => h.includes('cat'))

    const rows: ParsedRow[] = []
    for (let i = 1; i < lines.length; i++) {
      const parts = lines[i].split(',').map((p) => p.trim().replace(/^["']|["']$/g, ''))
      if (parts.length < 3) continue

      rows.push({
        id: idIdx !== -1 ? parts[idIdx] : `TXN_${Date.now()}_${i}`,
        vendor: vendorIdx !== -1 ? parts[vendorIdx] : parts[1] || 'Unknown Vendor',
        employee: empIdx !== -1 ? parts[empIdx] : parts[2] || 'Unknown Employee',
        amount: amtIdx !== -1 ? parseFloat(parts[amtIdx]) || 0 : 0,
        transactionTime: timeIdx !== -1 ? parts[timeIdx] : new Date().toISOString(),
        category: catIdx !== -1 ? parts[catIdx] : 'General',
      })
    }

    return rows
  }

  const handleFile = (selectedFile: File) => {
    if (!selectedFile.name.endsWith('.csv')) {
      setImportError('Please select a valid .csv file')
      return
    }

    setFile(selectedFile)
    setImportError(null)
    setImportSuccess(false)
    setAnalysisResult(null)
    setAnalysisError(null)

    const reader = new FileReader()
    reader.onload = (e) => {
      try {
        const text = e.target?.result as string
        const rows = parseCsvText(text)
        setParsedRows(rows)
      } catch (err) {
        setImportError(err instanceof Error ? err.message : 'Failed to parse CSV preview')
      }
    }
    reader.readAsText(selectedFile)
  }

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault()
    setIsDragging(true)
  }

  const handleDragLeave = () => {
    setIsDragging(false)
  }

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault()
    setIsDragging(false)
    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      handleFile(e.dataTransfer.files[0])
    }
  }

  const handleImport = async () => {
    if (!file) return

    try {
      setIsImporting(true)
      setImportError(null)
      setAnalysisResult(null)
      setAnalysisError(null)

      const result = await transactionsApi.importCsv(file)
      setImportedCount(result.count)
      setImportSuccess(true)
    } catch (err) {
      setImportError(err instanceof Error ? err.message : 'Import failed. Please check your CSV and try again.')
    } finally {
      setIsImporting(false)
    }
  }

  const handleRunAnalysis = async () => {
    try {
      setIsAnalyzing(true)
      setAnalysisError(null)
      const result = await riskApi.analyzeAll()
      setAnalysisResult(result)
    } catch (err) {
      setAnalysisError(err instanceof Error ? err.message : 'Failed to execute risk analysis. Please try again.')
    } finally {
      setIsAnalyzing(false)
    }
  }

  const handleReset = () => {
    setFile(null)
    setParsedRows([])
    setImportSuccess(false)
    setImportError(null)
    setAnalysisResult(null)
    setAnalysisError(null)
    setImportedCount(0)
  }

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-xl sm:text-2xl font-bold tracking-tight text-zinc-900 dark:text-zinc-100 flex items-center gap-2.5">
          <UploadCloud className="w-6 h-6 text-zinc-600 dark:text-zinc-400" />
          CSV Transaction Import & Risk Analysis
        </h1>
        <p className="text-xs sm:text-sm text-zinc-500 dark:text-zinc-400 mt-0.5">
          Ingest raw financial transaction datasets and run deterministic risk rules across the entire dataset
        </p>
      </div>

      {/* Upload Zone */}
      {!importSuccess ? (
        <div className="space-y-4">
          <div
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
            onDrop={handleDrop}
            onClick={() => fileInputRef.current?.click()}
            className={`p-10 rounded-2xl border-2 border-dashed text-center cursor-pointer transition-all duration-150 flex flex-col items-center justify-center gap-3 ${
              isDragging
                ? 'border-zinc-500 bg-zinc-100 dark:bg-zinc-800/80'
                : 'border-zinc-300 dark:border-zinc-700 bg-white/50 dark:bg-[#14161b]/50 hover:border-zinc-400 dark:hover:border-zinc-600'
            }`}
          >
            <input
              type="file"
              ref={fileInputRef}
              onChange={(e) => e.target.files && handleFile(e.target.files[0])}
              accept=".csv"
              className="hidden"
            />

            <div className="w-14 h-14 rounded-2xl bg-zinc-100 dark:bg-zinc-800 border border-zinc-200 dark:border-zinc-700 flex items-center justify-center text-zinc-500 dark:text-zinc-400 shadow-xs">
              <UploadCloud className="w-7 h-7" />
            </div>

            <div>
              <p className="text-sm font-semibold text-zinc-900 dark:text-zinc-100">
                Drop your transaction CSV here
              </p>
              <p className="text-xs text-zinc-400 dark:text-zinc-500 mt-0.5">
                or <span className="text-zinc-800 dark:text-zinc-200 underline font-medium">choose CSV</span> from your device
              </p>
            </div>

            <span className="text-[10px] text-zinc-400 font-mono">
              Accepted format: transaction_id, vendor, employee, amount, transaction_time, category
            </span>
          </div>

          {/* File Selected Preview Banner */}
          {file && (
            <div className="p-4 rounded-xl border border-zinc-200/80 dark:border-zinc-800/80 bg-white dark:bg-[#14161b] shadow-2xs flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-lg bg-zinc-100 dark:bg-zinc-800 flex items-center justify-center text-zinc-600 dark:text-zinc-300">
                  <FileText className="w-5 h-5" />
                </div>
                <div>
                  <h4 className="text-xs font-semibold text-zinc-900 dark:text-zinc-100 font-mono">
                    {file.name}
                  </h4>
                  <p className="text-[11px] text-zinc-400">
                    {(file.size / 1024).toFixed(1)} KB • {parsedRows.length} detected rows
                  </p>
                </div>
              </div>

              <div className="flex items-center gap-2">
                <button
                  onClick={() => {
                    setFile(null)
                    setParsedRows([])
                  }}
                  className="px-3 py-1.5 rounded-lg text-xs font-medium border border-zinc-200 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-zinc-700 dark:text-zinc-300 hover:bg-zinc-50 dark:hover:bg-zinc-700"
                >
                  Cancel
                </button>
                <button
                  onClick={handleImport}
                  disabled={isImporting}
                  className="inline-flex items-center gap-2 px-4 py-1.5 rounded-lg text-xs font-medium bg-zinc-900 dark:bg-zinc-100 text-white dark:text-zinc-900 hover:bg-zinc-800 dark:hover:bg-zinc-200 shadow-2xs disabled:opacity-50"
                >
                  <RefreshCw className={`w-3.5 h-3.5 ${isImporting ? 'animate-spin' : ''}`} />
                  {isImporting ? 'Importing...' : 'Import Transactions'}
                </button>
              </div>
            </div>
          )}

          {/* Sample CSV Data Preview Table */}
          {parsedRows.length > 0 && (
            <div className="rounded-xl border border-zinc-200/80 dark:border-zinc-800/80 bg-white dark:bg-[#14161b] shadow-2xs overflow-hidden">
              <div className="p-3.5 border-b border-zinc-200/80 dark:border-zinc-800/80 bg-zinc-50/50 dark:bg-zinc-900/40">
                <span className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                  Data Preview (First {Math.min(5, parsedRows.length)} rows)
                </span>
              </div>
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead className="bg-zinc-50/75 dark:bg-zinc-900/50 text-zinc-400">
                    <tr>
                      <th className="py-2.5 px-3.5 font-medium">ID</th>
                      <th className="py-2.5 px-3.5 font-medium">Vendor</th>
                      <th className="py-2.5 px-3.5 font-medium">Employee</th>
                      <th className="py-2.5 px-3.5 font-medium text-right">Amount</th>
                      <th className="py-2.5 px-3.5 font-medium">Category</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-zinc-100 dark:divide-zinc-800">
                    {parsedRows.slice(0, 5).map((row, i) => (
                      <tr key={i} className="hover:bg-zinc-50/50 dark:hover:bg-zinc-800/30">
                        <td className="py-2 px-3.5 font-mono text-zinc-900 dark:text-zinc-100">
                          {row.id}
                        </td>
                        <td className="py-2 px-3.5 text-zinc-700 dark:text-zinc-300">
                          {row.vendor}
                        </td>
                        <td className="py-2 px-3.5 text-zinc-700 dark:text-zinc-300">
                          {row.employee}
                        </td>
                        <td className="py-2 px-3.5 text-right font-mono text-zinc-900 dark:text-zinc-100">
                          {formatCurrency(row.amount)}
                        </td>
                        <td className="py-2 px-3.5 text-zinc-600 dark:text-zinc-400">
                          {row.category}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {importError && (
            <div className="p-4 rounded-xl border border-rose-200 dark:border-rose-900/60 bg-rose-50/50 dark:bg-rose-950/20 text-rose-700 dark:text-rose-400 text-xs flex items-center gap-2">
              <AlertCircle className="w-4 h-4 shrink-0" />
              <span>{importError}</span>
            </div>
          )}
        </div>
      ) : (
        /* Post-Import Workflow Area */
        <div className="space-y-6">
          {/* Step 1: Import Confirmed Card */}
          <div className="p-6 rounded-2xl border border-emerald-200/80 dark:border-emerald-900/60 bg-white dark:bg-[#14161b] shadow-xs flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div className="flex items-center gap-3.5">
              <div className="w-10 h-10 rounded-full bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 flex items-center justify-center shrink-0">
                <CheckCircle2 className="w-5 h-5" />
              </div>
              <div>
                <h3 className="text-sm font-bold text-zinc-900 dark:text-zinc-100">
                  {importedCount} transaction{importedCount !== 1 ? 's' : ''} imported
                </h3>
                <p className="text-xs text-zinc-500 dark:text-zinc-400 mt-0.5">
                  Records are saved in database. Run dataset-wide analysis to calculate risk scores and findings.
                </p>
              </div>
            </div>

            {!analysisResult && !isAnalyzing && (
              <button
                onClick={handleRunAnalysis}
                className="inline-flex items-center gap-2 px-4 py-2 rounded-lg text-xs font-semibold bg-indigo-600 hover:bg-indigo-700 text-white transition-colors shadow-2xs shrink-0"
              >
                <Play className="w-3.5 h-3.5 fill-current" />
                Run Risk Analysis
              </button>
            )}
          </div>

          {/* Step 2: Running Risk Analysis State */}
          {isAnalyzing && (
            <div className="p-8 rounded-2xl border border-indigo-200/80 dark:border-indigo-900/60 bg-white dark:bg-[#14161b] shadow-xs space-y-6">
              <div className="text-center space-y-1">
                <div className="inline-flex items-center gap-2 text-xs font-semibold uppercase tracking-wider text-indigo-600 dark:text-indigo-400">
                  <RefreshCw className="w-4 h-4 animate-spin" />
                  Running Risk Analysis
                </div>
                <h4 className="text-base font-bold text-zinc-900 dark:text-zinc-100">
                  Analyzing your transaction dataset
                </h4>
                <p className="text-xs text-zinc-500 dark:text-zinc-400">
                  Evaluating deterministic rules across all imported transactions
                </p>
              </div>

              {/* Visual Rule Evaluation Status */}
              <div className="max-w-md mx-auto space-y-2.5 bg-zinc-50 dark:bg-zinc-900/50 p-4 rounded-xl border border-zinc-200/60 dark:border-zinc-800/60 font-mono text-xs">
                <div className="flex items-center gap-2 text-emerald-600 dark:text-emerald-400">
                  <span>✓</span>
                  <span className="text-zinc-700 dark:text-zinc-300">High amount detection</span>
                </div>
                <div className="flex items-center gap-2 text-emerald-600 dark:text-emerald-400">
                  <span>✓</span>
                  <span className="text-zinc-700 dark:text-zinc-300">Unusual time detection</span>
                </div>
                <div className="flex items-center gap-2 text-emerald-600 dark:text-emerald-400">
                  <span>✓</span>
                  <span className="text-zinc-700 dark:text-zinc-300">Duplicate detection</span>
                </div>
                <div className="flex items-center gap-2 text-emerald-600 dark:text-emerald-400">
                  <span>✓</span>
                  <span className="text-zinc-700 dark:text-zinc-300">Round amount detection</span>
                </div>
                <div className="flex items-center gap-2 text-emerald-600 dark:text-emerald-400">
                  <span>✓</span>
                  <span className="text-zinc-700 dark:text-zinc-300">Transaction velocity</span>
                </div>
                <div className="flex items-center gap-2 text-indigo-600 dark:text-indigo-400 animate-pulse">
                  <span>●</span>
                  <span className="text-zinc-700 dark:text-zinc-300">Vendor concentration</span>
                </div>
              </div>
            </div>
          )}

          {/* Step 3: Analysis Error State */}
          {analysisError && (
            <div className="p-6 rounded-2xl border border-rose-200 dark:border-rose-900/60 bg-rose-50/50 dark:bg-rose-950/20 text-rose-700 dark:text-rose-400 space-y-3">
              <div className="flex items-center gap-2 text-xs font-semibold">
                <AlertCircle className="w-4 h-4 shrink-0" />
                <span>Analysis Failed</span>
              </div>
              <p className="text-xs text-rose-600 dark:text-rose-300">
                {analysisError}
              </p>
              <button
                onClick={handleRunAnalysis}
                className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium bg-rose-600 hover:bg-rose-700 text-white transition-colors shadow-2xs"
              >
                <RotateCcw className="w-3.5 h-3.5" />
                Retry
              </button>
            </div>
          )}

          {/* Step 4: Success State — Analysis Complete Summary */}
          {analysisResult && (
            <div className="p-8 rounded-2xl border border-zinc-200/80 dark:border-zinc-800/80 bg-white dark:bg-[#14161b] shadow-xs space-y-6">
              <div className="text-center space-y-1">
                <div className="inline-flex items-center gap-2 text-xs font-semibold uppercase tracking-wider text-emerald-600 dark:text-emerald-400">
                  <CheckCircle2 className="w-4 h-4" />
                  Analysis Complete
                </div>
                <h3 className="text-lg font-bold text-zinc-900 dark:text-zinc-100">
                  {analysisResult.transactionsAnalyzed} transactions analyzed
                </h3>
                <p className="text-xs text-zinc-500 dark:text-zinc-400">
                  Highest risk score detected:{' '}
                  <span className="font-semibold text-zinc-900 dark:text-zinc-100 font-mono">
                    {analysisResult.highestRiskScore}
                  </span>
                </p>
              </div>

              {/* Severity Breakdown Grid */}
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 max-w-lg mx-auto">
                <div className="p-3 rounded-xl border border-emerald-200/60 dark:border-emerald-900/40 bg-emerald-50/40 dark:bg-emerald-950/20 text-center">
                  <div className="text-xs font-medium text-emerald-700 dark:text-emerald-400">Low</div>
                  <div className="text-lg font-bold text-emerald-900 dark:text-emerald-100 font-mono mt-0.5">
                    {analysisResult.lowRisk}
                  </div>
                </div>

                <div className="p-3 rounded-xl border border-amber-200/60 dark:border-amber-900/40 bg-amber-50/40 dark:bg-amber-950/20 text-center">
                  <div className="text-xs font-medium text-amber-700 dark:text-amber-400">Medium</div>
                  <div className="text-lg font-bold text-amber-900 dark:text-amber-100 font-mono mt-0.5">
                    {analysisResult.mediumRisk}
                  </div>
                </div>

                <div className="p-3 rounded-xl border border-orange-200/60 dark:border-orange-900/40 bg-orange-50/40 dark:bg-orange-950/20 text-center">
                  <div className="text-xs font-medium text-orange-700 dark:text-orange-400">High</div>
                  <div className="text-lg font-bold text-orange-900 dark:text-orange-100 font-mono mt-0.5">
                    {analysisResult.highRisk}
                  </div>
                </div>

                <div className="p-3 rounded-xl border border-rose-200/60 dark:border-rose-900/40 bg-rose-50/40 dark:bg-rose-950/20 text-center">
                  <div className="text-xs font-medium text-rose-700 dark:text-rose-400">Critical</div>
                  <div className="text-lg font-bold text-rose-900 dark:text-rose-100 font-mono mt-0.5">
                    {analysisResult.criticalRisk}
                  </div>
                </div>
              </div>

              {/* Action CTAs */}
              <div className="pt-2 flex flex-wrap items-center justify-center gap-3">
                <button
                  onClick={() => navigate('/')}
                  className="inline-flex items-center gap-2 px-5 py-2.5 rounded-lg text-xs font-semibold bg-zinc-900 dark:bg-zinc-100 text-white dark:text-zinc-900 hover:bg-zinc-800 dark:hover:bg-zinc-200 transition-colors shadow-2xs"
                >
                  <LayoutDashboard className="w-4 h-4" />
                  View Dashboard
                </button>
                <button
                  onClick={() => navigate('/transactions')}
                  className="inline-flex items-center gap-2 px-4 py-2.5 rounded-lg text-xs font-semibold border border-zinc-200 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-zinc-700 dark:text-zinc-300 hover:bg-zinc-50 dark:hover:bg-zinc-700 transition-colors shadow-2xs"
                >
                  <Layers className="w-4 h-4" />
                  View Transactions
                </button>
                <button
                  onClick={handleReset}
                  className="px-4 py-2.5 rounded-lg text-xs font-medium text-zinc-500 dark:text-zinc-400 hover:text-zinc-800 dark:hover:text-zinc-200"
                >
                  Upload Another CSV
                </button>
              </div>
            </div>
          )}
        </div>
      )}

      {/* Information Callout */}
      <div className="p-4 rounded-xl border border-zinc-200/60 dark:border-zinc-800/60 bg-zinc-50/50 dark:bg-zinc-900/30 flex items-start gap-3 text-xs text-zinc-500 dark:text-zinc-400">
        <Info className="w-4 h-4 text-zinc-400 shrink-0 mt-0.5" />
        <p className="leading-relaxed">
          <strong className="text-zinc-700 dark:text-zinc-300 font-medium">
            Audit Integrity Notice:
          </strong>{' '}
          All imported records are evaluated using deterministic risk rules (Round Amount, Unusual Time, Velocity, Concentration, High Amount, and Duplicates). Analysis history is preserved across runs.
        </p>
      </div>
    </div>
  )
}

