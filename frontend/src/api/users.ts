/**
 * Sprint 2B.6 – Admin User Management API Client
 */

import { apiClient } from './client'
import type { ManagedUser, CreateUserData, UpdateUserStatusData } from '../types/auth'

export const usersApi = {
  /**
   * GET /api/users
   * Returns a list of all users. Accessible by ADMIN only.
   */
  getUsers: async (): Promise<ManagedUser[]> => {
    const response = await apiClient.get<ManagedUser[]>('/users')
    return response.data
  },

  /**
   * POST /api/users
   * Creates a new user with role and credentials. Accessible by ADMIN only.
   */
  createUser: async (data: CreateUserData): Promise<ManagedUser> => {
    const response = await apiClient.post<ManagedUser>('/users', data)
    return response.data
  },

  /**
   * PATCH /api/users/{id}/status
   * Updates an existing user's enabled status. Accessible by ADMIN only.
   */
  updateUserStatus: async (id: number, data: UpdateUserStatusData): Promise<ManagedUser> => {
    const response = await apiClient.patch<ManagedUser>(`/users/${id}/status`, data)
    return response.data
  },
}
