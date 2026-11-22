import { Link } from 'react-router-dom'

/**
 * Placeholder so "Proceed to checkout" has somewhere to go. The route is protected,
 * so guests are asked to log in first. Payment and the order form arrive in Sprint 3.
 */
export default function CheckoutPage() {
  return (
    <section>
      <h1>Checkout</h1>
      <p>Checkout is not available yet.</p>
      <Link to="/cart">Back to cart</Link>
    </section>
  )
}
