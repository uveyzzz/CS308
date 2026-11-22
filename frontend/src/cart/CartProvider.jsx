import { useContext, useEffect, useRef, useState } from 'react'
import { addCartItem, clearSavedCart, fetchCart, mergeCart, removeCartItem, setCartQuantity } from '../api/cart'
import { AuthContext } from '../auth/authContext'
import { CartContext } from './cartContext'
import {
  CART_KEY,
  addLine,
  countItems,
  parseStoredCart,
  removeLine,
  setLineQuantity,
  subtotal,
} from './cartLogic'

/**
 * The cart, in one of two places (req. 4):
 * - Guests (and staff, who cannot buy) keep it in localStorage, so it survives a refresh.
 * - A logged-in customer's cart is saved on the backend and follows them to any device (SCRUM-24).
 *
 * When a customer logs in, the guest cart is merged into the saved cart once and then cleared
 * from the browser. Quantities of the same variant are added up, capped at the stock.
 *
 * A line is one VARIANT (size + color), so 30ml and 50ml of the same product are separate lines.
 * The cart logic itself lives in cartLogic.js; this file only connects it to React and the API.
 */
export default function CartProvider({ children }) {
  // Read the context directly instead of useAuth(), so the cart also works without an
  // AuthProvider around it (its own tests render it alone).
  const user = useContext(AuthContext)?.user ?? null
  const customerId = user?.role === 'CUSTOMER' ? user.id : null
  const isCustomer = customerId !== null

  const [guestItems, setGuestItems] = useState(() => parseStoredCart(localStorage.getItem(CART_KEY)))
  // The saved cart, tagged with its owner, so another customer never sees it, even for a moment.
  const [saved, setSaved] = useState({ customerId: null, lines: [] })
  // The merge or load for the current customer. Kept across renders so React's development
  // double render cannot merge the same guest cart twice.
  const syncRef = useRef(null)

  useEffect(() => {
    if (!isCustomer) localStorage.setItem(CART_KEY, JSON.stringify(guestItems))
  }, [guestItems, isCustomer])

  // Keep two open tabs in sync: a change in one tab shows up in the other.
  useEffect(() => {
    function handleStorage(event) {
      if (event.key === CART_KEY) setGuestItems(parseStoredCart(event.newValue))
    }
    window.addEventListener('storage', handleStorage)
    return () => window.removeEventListener('storage', handleStorage)
  }, [])

  useEffect(() => {
    if (customerId === null) {
      syncRef.current = null
      return undefined
    }
    if (syncRef.current?.customerId !== customerId) {
      const guestLines = parseStoredCart(localStorage.getItem(CART_KEY))
      const promise =
        guestLines.length > 0
          ? mergeCart(guestLines.map(({ variantId, quantity }) => ({ variantId, quantity }))).then((lines) => {
              // Only now is the guest cart safe on the backend, so only now is it removed.
              localStorage.removeItem(CART_KEY)
              return lines
            })
          : fetchCart()
      syncRef.current = { customerId, promise }
    }
    let cancelled = false
    syncRef.current.promise
      .then((lines) => {
        if (cancelled) return
        setSaved({ customerId, lines })
        setGuestItems([])
      })
      // If the merge fails, the guest cart is still in localStorage and is tried again next time.
      .catch(() => {})
    return () => {
      cancelled = true
    }
  }, [customerId])

  /** Sends a change to the backend and shows the cart it answers with. */
  function saveChange(request) {
    const show = (lines) => setSaved({ customerId, lines })
    return request.then(show).catch(() =>
      fetchCart()
        .then(show)
        .catch(() => {}),
    )
  }

  // Until the saved cart arrives, keep showing the guest lines being merged, so the cart
  // does not flash empty right after login.
  const items = isCustomer && saved.customerId === customerId ? saved.lines : guestItems

  const value = {
    items,
    count: countItems(items),
    subtotal: subtotal(items),
    addItem: (item, quantity, stock) =>
      isCustomer
        ? saveChange(addCartItem(item.variantId, quantity))
        : setGuestItems((current) => addLine(current, item, quantity, stock)),
    updateQuantity: (variantId, quantity) => {
      if (!isCustomer) return setGuestItems((current) => setLineQuantity(current, variantId, quantity))
      const whole = Math.floor(quantity)
      return saveChange(whole > 0 ? setCartQuantity(variantId, whole) : removeCartItem(variantId))
    },
    removeItem: (variantId) =>
      isCustomer
        ? saveChange(removeCartItem(variantId))
        : setGuestItems((current) => removeLine(current, variantId)),
    clearCart: () =>
      isCustomer ? saveChange(clearSavedCart().then(() => [])) : setGuestItems([]),
  }

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>
}
