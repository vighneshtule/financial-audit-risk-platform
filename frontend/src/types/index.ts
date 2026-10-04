export type RiskSeverity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'

export type RiskType =
  | 'HIGH_AMOUNT'
  | 'UNUSUAL_TRANSACTION_TIME'
  | 'DUPLICATE_TRANSACTION'
  | 'ROUND_AMOUNT'
  | 'TRANSACTION_VELOCITY'
  | 'VENDOR_CONCENTRATION'

export interface RiskFinding {
  type: RiskType
  score: number
  severity: RiskSeverity
  explanation: string
}

export interface Transaction {
  id: string
  vendor: string
  employee: string
  amount: number
  transactionTime: string
  category: string
}

export interface RiskTransactionResponse {
  transactionId: string
  vendor: string
  employee: string
  amount: number
  category: string
  riskScore: number
  riskLevel: RiskSeverity
  findings: RiskFinding[]
}

export interface RiskTransactionPage {
  content: RiskTransactionResponse[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface RiskSummary {
  totalTransactions: number
  totalAmount: number
  lowRiskTransactions: number
  mediumRiskTransactions: number
  highRiskTransactions: number
  criticalRiskTransactions: number
  totalFindings: number
  highestRiskTransactionId: string | null
  highestRiskScore: number
}

export interface RiskReport {
  riskScore: number
  riskLevel: RiskSeverity
  findings: RiskFinding[]
}

export interface RiskAnalysisHistoryItem {
  analysisRunId: number
  analyzedAt: string
  riskScore: number
  riskLevel: RiskSeverity
  findings: RiskFinding[]
}

export interface RiskAnalysisHistoryResponse {
  transactionId: string
  analysisRuns: RiskAnalysisHistoryItem[]
}

export interface RiskAnalysisHistoryPage {
  transactionId: string
  content: RiskAnalysisHistoryItem[]
  page: number
  size: number
  totalElements: number
}

export interface TransactionFilters {
  riskLevel?: RiskSeverity
  minScore?: number
  page?: number
  size?: number
  search?: string
}

export interface AnalysisResult {
  transactionsAnalyzed: number
  lowRisk: number
  mediumRisk: number
  highRisk: number
  criticalRisk: number
  highestRiskScore: number
}

export interface DatasetOverview {
  totalTransactions: number
  analyzedTransactions: number
  totalAuditValue: number
  totalFindings: number
  lowRisk: number
  mediumRisk: number
  highRisk: number
  criticalRisk: number
  highestRiskScore: number
}

export interface SeverityCount {
  severity: RiskSeverity | string
  count: number
}

export interface VendorRisk {
  vendor: string
  transactionCount: number
  flaggedTransactionCount: number
  totalAmount: number
  highestRiskScore: number
  findingCount: number
}

export interface EmployeeRisk {
  employee: string
  transactionCount: number
  flaggedTransactionCount: number
  totalAmount: number
  highestRiskScore: number
  findingCount: number
}

export interface RiskTypeDistribution {
  riskType: string
  findingCount: number
  totalScoreContribution: number
}

export interface CategoryRisk {
  category: string
  transactionCount: number
  flaggedTransactionCount: number
  totalAmount: number
  findingCount: number
}

export interface RiskIntelligenceSummary {
  overview: DatasetOverview
  severityDistribution: SeverityCount[]
  vendors: VendorRisk[]
  employees: EmployeeRisk[]
  riskTypes: RiskTypeDistribution[]
  categories: CategoryRisk[]
}

export interface RelatedTransaction {
  transactionId: string
  amount: number
  transactionTime: string | null
  riskScore: number | null
  riskLevel: RiskSeverity | null
}

export interface RelatedGroup {
  name: string
  relatedTransactionCount: number
  totalAmount: number
  highestRiskScore: number
  flaggedTransactionCount: number
  transactions: RelatedTransaction[]
}

export interface EvidenceSummary {
  relatedTransactionCount: number
  relatedFlaggedTransactionCount: number
  relatedAmount: number
}

export interface TransactionContext {
  transactionId: string
  vendor: RelatedGroup
  employee: RelatedGroup
  category: RelatedGroup
  evidenceSummary: EvidenceSummary
}

export type AuditDecisionType =
  | 'CONFIRMED_RISK'
  | 'FALSE_POSITIVE'
  | 'REQUIRES_INVESTIGATION'
  | 'ESCALATED'

export interface AuditDecision {
  id: number
  transactionId: string
  analysisRunId: number
  decision: AuditDecisionType
  comment: string | null
  decidedBy: string
  decidedAt: string
}

export interface AuditEvent {
  id: number
  transactionId: string
  decisionId: number | null
  eventType: string
  eventDetails: string
  actor: string
  createdAt: string
}

export interface CreateAuditDecisionRequest {
  analysisRunId?: number
  decision: AuditDecisionType
  comment?: string
  decidedBy: string
}
