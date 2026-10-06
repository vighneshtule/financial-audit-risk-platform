/**
 * Sprint 2B.1 – Authentication Foundation
 * Auth API methods – login and /me endpoint.
 */

import { apiClient } from './client'
import type { AuthUser, LoginResponse } from '../types/auth'

export const authApi = {
  /**
   * POST /api/auth/login
   * Authenticates the user and returns a JWT token + user info.
   */
  login: async (username: string, password: string): Promise<LoginResponse> => {
    const response = await apiClient.post<LoginResponse>('/auth/login', {
      username,
      password,
    })
    return response.data
  },

  /**
   * GET /api/auth/me
   * Validates the current token and returns the authenticated user.
   * The Bearer token is injected automatically by the request interceptor.
   */
  me: async (): Promise<AuthUser> => {
    const response = await apiClient.get<AuthUser>('/auth/me')
    return response.data
  },
}
