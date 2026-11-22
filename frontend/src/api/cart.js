import { apiFetch } from './client'

// The logged-in customer's saved cart (SCRUM-24). Every call answers with the whole cart:
// [{ variantId, productId, slug, name, size, color, price, image, quantity, maxStock }]

export function fetchCart() {
  return apiFetch('/api/cart')
}

/** Sends the guest cart once, right after login. lines: [{ variantId, quantity }] */
export function mergeCart(lines) {
  return apiFetch('/api/cart/merge', { method: 'POST', body: { items: lines } })
}

export function addCartItem(variantId, quantity) {
  return apiFetch('/api/cart/items', { method: 'POST', body: { variantId, quantity } })
}

export function setCartQuantity(variantId, quantity) {
  return apiFetch(`/api/cart/items/${variantId}`, { method: 'PUT', body: { quantity } })
}

export function removeCartItem(variantId) {
  return apiFetch(`/api/cart/items/${variantId}`, { method: 'DELETE' })
}

export function clearSavedCart() {
  return apiFetch('/api/cart', { method: 'DELETE' })
}
