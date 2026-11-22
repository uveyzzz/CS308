import { useState } from 'react'
import { Link, NavLink, Outlet } from 'react-router-dom'
import { ADMIN_AREAS } from '../auth/adminAreas'
import { useAuth } from '../auth/authContext'
import { useCart } from '../cart/cartContext'
import CategoryMenu from './CategoryMenu'

export default function Layout() {
  const { user, loading, logout } = useAuth()
  const { count } = useCart()
  const adminArea = ADMIN_AREAS.find(({ role }) => role === user?.role)
  // On narrow screens the links fold behind a Menu button (SCRUM-28). On wider screens
  // CSS always shows them and hides the button.
  const [menuOpen, setMenuOpen] = useState(false)

  // Following any link in the menu closes it.
  function closeMenuOnLink(event) {
    if (event.target.closest('a')) setMenuOpen(false)
  }

  return (
    <div className="app">
      <header className="app-header">
        <Link to="/" className="brand">
          CS308 Store
        </Link>
        <button
          type="button"
          className="menu-toggle"
          aria-expanded={menuOpen}
          aria-controls="site-menu"
          onClick={() => setMenuOpen((open) => !open)}
        >
          Menu
        </button>
        <div id="site-menu" className={`header-menu${menuOpen ? ' header-menu-open' : ''}`} onClick={closeMenuOnLink}>
          <nav>
            <NavLink to="/" end>
              Home
            </NavLink>
            <NavLink to="/products">Products</NavLink>
            <NavLink to="/cart" className="cart-count">
              Cart ({count})
            </NavLink>
          </nav>
          <CategoryMenu />
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
        </div>
      </header>

      <main className="app-main">
        <Outlet />
      </main>

      <footer className="app-footer">CS308 Online Store Project</footer>
    </div>
  )
}
