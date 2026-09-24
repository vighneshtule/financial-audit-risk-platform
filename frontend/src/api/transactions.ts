import { apiClient } from './client'
import type { Transaction } from '../types'

export const transactionsApi = {
  getAll: async (): Promise<Transaction[]> => {
    const response = await apiClient.get<Transaction[]>('/transactions')
    return response.data
  },
}
