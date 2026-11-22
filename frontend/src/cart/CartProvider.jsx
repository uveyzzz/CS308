import { useEffect, useState } from 'react'
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
 * Guest cart, kept in localStorage so it survives a refresh (req. 4).
 * A line is one VARIANT (size + color), so 30ml and 50ml of the same product are separate lines.
 * The quantity never goes above the stock seen when the variant was added.
 * The cart logic itself lives in cartLogic.js; this file only connects it to React.
 */
export default function CartProvider({ children }) {
  const [items, setItems] = useState(() => parseStoredCart(localStorage.getItem(CART_KEY)))

  useEffect(() => {
    localStorage.setItem(CART_KEY, JSON.stringify(items))
  }, [items])

  // Keep two open tabs in sync: a change in one tab shows up in the other.
  useEffect(() => {
    function handleStorage(event) {
      if (event.key === CART_KEY) setItems(parseStoredCart(event.newValue))
    }
    window.addEventListener('storage', handleStorage)
    return () => window.removeEventListener('storage', handleStorage)
  }, [])

  const value = {
    items,
    count: countItems(items),
    subtotal: subtotal(items),
    addItem: (item, quantity, stock) => setItems((current) => addLine(current, item, quantity, stock)),
    updateQuantity: (variantId, quantity) =>
      setItems((current) => setLineQuantity(current, variantId, quantity)),
    removeItem: (variantId) => setItems((current) => removeLine(current, variantId)),
    clearCart: () => setItems([]),
  }

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>
}
