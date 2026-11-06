import { useCallback, useEffect, useMemo, useState } from 'react'
import { fetchCurrentUser, loginUser, registerUser, updateProfile } from '../api/auth'
import { SESSION_EXPIRED_EVENT, getToken, setToken } from '../api/client'
import { AuthContext } from './authContext'

// Keeps track of who is logged in. The token stays in localStorage, so a page refresh
// only has to ask the backend who that token belongs to.
export default function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(() => getToken() !== null)

  useEffect(() => {
    if (!getToken()) return undefined
    let cancelled = false
    fetchCurrentUser()
      .then((currentUser) => {
        if (!cancelled) setUser(currentUser)
      })
      // A rejected token is already cleared by apiFetch; other failures leave a guest view.
      .catch(() => {})
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [])

  useEffect(() => {
    const onSessionExpired = () => setUser(null)
    window.addEventListener(SESSION_EXPIRED_EVENT, onSessionExpired)
    return () => window.removeEventListener(SESSION_EXPIRED_EVENT, onSessionExpired)
  }, [])

  const startSession = useCallback(({ token, user: loggedInUser }) => {
    setToken(token)
    setUser(loggedInUser)
  }, [])

  const value = useMemo(
    () => ({
      user,
      loading,
      login: async (credentials) => startSession(await loginUser(credentials)),
      register: async (data) => startSession(await registerUser(data)),
      saveProfile: async (changes) => setUser(await updateProfile(changes)),
      logout: () => {
        setToken(null)
        setUser(null)
      },
    }),
    [user, loading, startSession],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
