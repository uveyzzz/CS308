import { useEffect, useState } from 'react'
import { CartContext } from './cartContext'

const CART_KEY = 'cart'

function loadCart() {
  try {
    return JSON.parse(localStorage.getItem(CART_KEY)) ?? []
  } catch {
    return []
  }
}

/**
 * Client-side cart. A line is one VARIANT (size + color), so 30ml and 50ml of the
 * same product are separate lines. The quantity never goes above the variant's stock.
 */
export default function CartProvider({ children }) {
  const [items, setItems] = useState(loadCart)

  useEffect(() => {
    localStorage.setItem(CART_KEY, JSON.stringify(items))
  }, [items])

  function addItem(item, quantity, stock) {
    setItems((current) => {
      const existing = current.find((line) => line.variantId === item.variantId)
      if (existing) {
        return current.map((line) =>
          line.variantId === item.variantId
            ? { ...line, quantity: Math.min(line.quantity + quantity, stock) }
            : line,
        )
      }
      return [...current, { ...item, quantity: Math.min(quantity, stock) }]
    })
  }

  const count = items.reduce((total, line) => total + line.quantity, 0)

  return <CartContext.Provider value={{ items, addItem, count }}>{children}</CartContext.Provider>
}
