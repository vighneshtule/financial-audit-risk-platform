import { apiClient } from './client'
import type {
  RiskSummary,
  RiskReport,
  RiskFinding,
  RiskTransactionPage,
  RiskAnalysisHistoryResponse,
  RiskAnalysisHistoryItem,
  RiskAnalysisHistoryPage,
  RiskSeverity,
  AnalysisResult,
  RiskIntelligenceSummary,
  TransactionContext,
  AuditDecision,
  AuditEvent,
  CreateAuditDecisionRequest,
} from '../types'

export const riskApi = {
  getTransactionContext: async (transactionId: string): Promise<TransactionContext> => {
    const response = await apiClient.get<TransactionContext>(`/risk/transactions/${transactionId}/context`)
    return response.data
  },

  createAuditDecision: async (
    transactionId: string,
    payload: CreateAuditDecisionRequest
  ): Promise<AuditDecision> => {
    const response = await apiClient.post<AuditDecision>(
      `/risk/transactions/${transactionId}/audit/decisions`,
      payload
    )
    return response.data
  },

  getLatestAuditDecision: async (transactionId: string): Promise<AuditDecision | null> => {
    try {
      const response = await apiClient.get<AuditDecision | null>(
        `/risk/transactions/${transactionId}/audit/decision`
      )
      return response.data || null
    } catch {
      return null
    }
  },

  getAuditDecisions: async (transactionId: string): Promise<AuditDecision[]> => {
    const response = await apiClient.get<AuditDecision[]>(
      `/risk/transactions/${transactionId}/audit/decisions`
    )
    return response.data || []
  },

  getAuditEvents: async (transactionId: string): Promise<AuditEvent[]> => {
    const response = await apiClient.get<AuditEvent[]>(
      `/risk/transactions/${transactionId}/audit/events`
    )
    return response.data || []
  },


  getIntelligenceSummary: async (): Promise<RiskIntelligenceSummary> => {

    const response = await apiClient.get<RiskIntelligenceSummary>('/risk/intelligence/summary')
    return response.data
  },

  analyzeAll: async (): Promise<AnalysisResult> => {
    const response = await apiClient.post<AnalysisResult>('/risk/analyze-all')
    return response.data
  },

  getSummary: async (): Promise<RiskSummary> => {
    const response = await apiClient.get<RiskSummary>('/risk/summary')
    return response.data
  },

  getTransactions: async (params?: {
    riskLevel?: RiskSeverity
    minScore?: number
    page?: number
    size?: number
  }): Promise<RiskTransactionPage> => {
    const response = await apiClient.get<RiskTransactionPage>('/risk/transactions', {
      params,
    })
    return response.data
  },

  analyzeTransaction: async (transactionId: string): Promise<RiskReport> => {
    const response = await apiClient.get<RiskReport>(`/risk/transactions/${transactionId}`)
    return response.data
  },

  analyzeAndPersist: async (transactionId: string): Promise<RiskReport> => {
    const response = await apiClient.post<RiskReport>(`/risk/analyze/${transactionId}`)
    return response.data
  },

  getPersistedFindings: async (transactionId: string): Promise<RiskFinding[]> => {
    const response = await apiClient.get<RiskFinding[]>(`/risk/transactions/${transactionId}/findings`)
    return response.data
  },

  getTransactionHistory: async (transactionId: string): Promise<RiskAnalysisHistoryResponse> => {
    const response = await apiClient.get<RiskAnalysisHistoryResponse>(
      `/risk/transactions/${transactionId}/history`
    )
    return response.data
  },

  getHistoryRun: async (
    transactionId: string,
    analysisRunId: number
  ): Promise<RiskAnalysisHistoryItem> => {
    const response = await apiClient.get<RiskAnalysisHistoryItem>(
      `/risk/transactions/${transactionId}/history/${analysisRunId}`
    )
    return response.data
  },

  getHistoryPage: async (
    transactionId: string,
    page: number = 0,
    size: number = 10
  ): Promise<RiskAnalysisHistoryPage> => {
    const response = await apiClient.get<RiskAnalysisHistoryPage>(
      `/risk/transactions/${transactionId}/history/page`,
      {
        params: { page, size },
      }
    )
    return response.data
  },
}
