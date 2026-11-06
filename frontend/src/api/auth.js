import { apiFetch } from './client'

export function registerUser(data) {
  return apiFetch('/api/auth/register', { method: 'POST', body: data })
}

export function loginUser(credentials) {
  return apiFetch('/api/auth/login', { method: 'POST', body: credentials })
}

export function fetchCurrentUser() {
  return apiFetch('/api/me')
}

export function updateProfile(changes) {
  return apiFetch('/api/me', { method: 'PATCH', body: changes })
}
