import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from './authContext'

// Wrap routes that need a login. Guests go to /login and come back here afterwards.
// With roles, a logged-in user with another role goes to the home page.
// This only decides what to show; the backend still protects the data itself.
export default function ProtectedRoute({ roles }) {
  const { user, loading } = useAuth()
  const location = useLocation()

  if (loading) return <p role="status">Loading…</p>
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />
  if (roles && !roles.includes(user.role)) return <Navigate to="/" replace />
  return <Outlet />
}
