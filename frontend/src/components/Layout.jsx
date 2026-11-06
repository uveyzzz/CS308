import { useState } from 'react'
import { Link, Outlet } from 'react-router-dom'

export default function Layout() {
  const [isMenuOpen, setIsMenuOpen] = useState(false)
  const [isCartOpen, setIsCartOpen] = useState(false)
  // Örnek kullanıcı durumu (Backend entegrasyonu gelene kadar mock)
  const [user, setUser] = useState(null) // Giriş yapınca: { name: 'Elif' }

  return (
    <div className="layout-wrapper">
      {/* GLOBAL HEADER */}
      <header className="site-header">
        <button 
          className="header-icon-btn" 
          onClick={() => setIsMenuOpen(true)}
          aria-label="Menüyü Aç"
        >
          ☰
        </button>

        <div className="search-bar-container">
          <input 
            type="text" 
            placeholder="Koku veya ürün ara..." 
            className="search-input"
          />
        </div>

        <div className="header-right">
          {user ? (
            <Link to="/account" className="user-link">
              Merhaba, {user.name}
            </Link>
          ) : (
            <Link to="/login" className="login-link">
              Giriş Yap / Üye Ol
            </Link>
          )}

          <button 
            className="cart-btn" 
            onClick={() => setIsCartOpen(true)}
            aria-label="Sepeti Gör"
          >
            🛒
            <span className="cart-count">2</span>
          </button>
        </div>
      </header>

      {/* SOL YAN MENÜ (SIDEBAR) */}
      {isMenuOpen && (
        <div className="sidebar-overlay" onClick={() => setIsMenuOpen(false)}>
          <aside className="sidebar-content" onClick={(e) => e.stopPropagation()}>
            <button className="close-btn" onClick={() => setIsMenuOpen(false)}>✕</button>
            <h2 className="brand-logo">NŪMA</h2>
            <nav className="menu-links">
              <Link to="/" onClick={() => setIsMenuOpen(false)}>Koleksiyon</Link>
              <Link to="/products" onClick={() => setIsMenuOpen(false)}>Parfümler</Link>
              <Link to="/about" onClick={() => setIsMenuOpen(false)}>Hakkımızda</Link>
              <Link to="/contact" onClick={() => setIsMenuOpen(false)}>İletişim</Link>
            </nav>
          </aside>
        </div>
      )}

      {/* SAĞ SEPET ÇEKMECESİ (DRAWER) */}
      {isCartOpen && (
        <div className="cart-overlay" onClick={() => setIsCartOpen(false)}>
          <aside className="cart-drawer" onClick={(e) => e.stopPropagation()}>
            <div className="cart-header">
              <h3>SEPETİM (2)</h3>
              <button onClick={() => setIsCartOpen(false)}>✕</button>
            </div>
            <div className="cart-items">
              {/* Örnek Sepet İtemi */}
              <div className="cart-item">
                <p>Scent No. 04 / Oud & Amber</p>
                <span>$120.00</span>
              </div>
            </div>
            <button className="checkout-btn">SİPARİŞİ TAMAMLA (CHECKOUT)</button>
          </aside>
        </div>
      )}

      {/* SAYFA İÇERİKLERİ BURAYA YÜKLENİR */}
      <main className="main-content">
        <Outlet context={{ user, setUser }} />
      </main>
    </div>
  )
}