import { Link, NavLink, Outlet } from 'react-router-dom'
import { ADMIN_AREAS } from '../auth/adminAreas'
import { useAuth } from '../auth/authContext'

export default function Layout() {
  const { user, loading, logout } = useAuth()
  const adminArea = ADMIN_AREAS.find(({ role }) => role === user?.role)

  return (
    <div className="app">
      <header className="app-header">
        <Link to="/" className="brand">
          CS308 Store
        </Link>
        <nav>
          <NavLink to="/" end>
            Home
          </NavLink>
          {/* Sprint 2: add Products and Cart links as those pages land. */}
        </nav>
        {!loading && (
          <nav className="app-header-account" aria-label="Account">
            {user ? (
              <>
                {adminArea && <NavLink to={adminArea.path}>{adminArea.label}</NavLink>}
                <NavLink to="/account/profile">My profile</NavLink>
                <button type="button" className="link-button" onClick={logout}>
                  Log out
                </button>
              </>
            ) : (
              <>
                <NavLink to="/login">Log in</NavLink>
                <NavLink to="/register">Create account</NavLink>
              </>
            )}
          </nav>
        )}
      </header>

      <main className="app-main">
        <Outlet />
      </main>

      <footer className="app-footer">CS308 Online Store Project</footer>
    </div>
  )
}
