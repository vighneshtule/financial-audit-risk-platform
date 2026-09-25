import { apiClient } from './client'
import type { Transaction } from '../types'

export interface ImportResponse {
  message: string
  count: number
}

export const transactionsApi = {
  getAll: async (): Promise<Transaction[]> => {
    const response = await apiClient.get<Transaction[]>('/transactions')
    return response.data
  },

  importCsv: async (file: File): Promise<ImportResponse> => {
    const formData = new FormData()
    formData.append('file', file)
    const response = await apiClient.post<ImportResponse>(
      '/transactions/import',
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      }
    )
    return response.data
  },
}

