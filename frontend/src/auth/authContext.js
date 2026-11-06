import { createContext, useContext } from 'react'

export const AuthContext = createContext(null)

/** Gives any component { user, loading, login, register, logout, saveProfile }. */
export function useAuth() {
  const auth = useContext(AuthContext)
  if (!auth) throw new Error('useAuth must be used inside AuthProvider')
  return auth
}
