import { createContext, useContext } from 'react'

export const CartContext = createContext(null)

/**
 * Gives any component { items, count, subtotal, addItem, updateQuantity, removeItem, clearCart }.
 * addItem(item, quantity, stock), updateQuantity(variantId, quantity), removeItem(variantId).
 */
export function useCart() {
  const value = useContext(CartContext)
  if (!value) throw new Error('useCart must be used inside <CartProvider>')
  return value
}
