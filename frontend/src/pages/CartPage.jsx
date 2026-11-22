import { Link } from 'react-router-dom'
import { useAuth } from '../auth/authContext'
import { useCart } from '../cart/cartContext'
import { lineTotal } from '../cart/cartLogic'
import { formatPrice } from '../utils/money'
import './cart.css'

/** Shopping cart page (req. 4). Guests can see and edit the cart; checkout needs a login. */
export default function CartPage() {
  const { items, count, subtotal, updateQuantity, removeItem } = useCart()
  const { user, loading } = useAuth()
  // While a saved login is still being checked, /checkout's own guard decides.
  const isGuest = !user && !loading

  if (items.length === 0) {
    return (
      <section className="cart-empty">
        <h1>Your cart</h1>
        <p>Your cart is empty.</p>
        <Link to="/products">Browse products</Link>
      </section>
    )
  }

  return (
    <section>
      <h1>Your cart</h1>
      <div className="cart">
        <ul className="cart-lines" aria-label="Cart items">
          {items.map((line) => (
            <CartLine key={line.variantId} line={line} onChange={updateQuantity} onRemove={removeItem} />
          ))}
        </ul>

        <aside className="cart-summary" aria-label="Order summary">
          <p className="cart-summary-row">
            <span>Subtotal ({count} {count === 1 ? 'item' : 'items'})</span>
            <strong aria-label="Subtotal">{formatPrice(subtotal)}</strong>
          </p>
          {/* A guest logs in first and comes back to the cart, now merged into their saved cart (SCRUM-24). */}
          <Link
            to={isGuest ? '/login' : '/checkout'}
            state={isGuest ? { from: '/cart' } : undefined}
            className="primary-button cart-checkout"
          >
            Proceed to checkout
          </Link>
          {!user && <p className="cart-note">You will need to log in to check out.</p>}
        </aside>
      </div>
    </section>
  )
}

function CartLine({ line, onChange, onRemove }) {
  const label = `${line.name} (${line.size} / ${line.color})`
  const atStock = Number.isFinite(line.maxStock) && line.quantity >= line.maxStock

  return (
    <li className="cart-line">
      {line.image ? <img className="cart-line-image" src={line.image} alt={line.name} /> : <div className="cart-line-image" />}
      <div className="cart-line-info">
        <Link to={`/product/${line.slug}`} className="cart-line-name">
          {line.name}
        </Link>
        <p className="cart-line-variant">
          Size: {line.size} · Color: {line.color}
        </p>
        <p className="cart-line-price">{formatPrice(line.price)} each</p>
      </div>
      <div className="quantity">
        <button
          type="button"
          aria-label={`Decrease quantity of ${label}`}
          disabled={line.quantity <= 1}
          onClick={() => onChange(line.variantId, line.quantity - 1)}
        >
          −
        </button>
        <span aria-label={`Quantity of ${label}`}>{line.quantity}</span>
        <button
          type="button"
          aria-label={`Increase quantity of ${label}`}
          disabled={atStock}
          onClick={() => onChange(line.variantId, line.quantity + 1)}
        >
          +
        </button>
      </div>
      <strong className="cart-line-total" aria-label={`Total for ${label}`}>
        {formatPrice(lineTotal(line))}
      </strong>
      <button type="button" className="link-button" aria-label={`Remove ${label}`} onClick={() => onRemove(line.variantId)}>
        Remove
      </button>
    </li>
  )
}
