import { Link, NavLink, Outlet } from 'react-router-dom'

export default function Layout() {
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
          {/* Sprint 1: add Products, Cart and Login links as those pages land. */}
        </nav>
      </header>

      <main className="app-main">
        <Outlet />
      </main>

      <footer className="app-footer">CS308 Online Store Project</footer>
    </div>
  )
}
