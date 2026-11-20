// Pure cart functions: they take the current lines and return new ones, never change
// the input and never touch localStorage. CartProvider wires them to React state.
//
// A line is one variant (size + color):
// { variantId, productId, slug, name, size, color, price, image, quantity, maxStock }
// maxStock is the stock seen when the line was added; quantities never go above it.

export const CART_KEY = 'cart'

const toCents = (amount) => Math.round(amount * 100)

function capQuantity(quantity, maxStock) {
  return Number.isFinite(maxStock) ? Math.min(quantity, maxStock) : quantity
}

function isValidLine(line) {
  return (
    line !== null &&
    typeof line === 'object' &&
    (typeof line.variantId === 'number' || typeof line.variantId === 'string') &&
    Number.isInteger(line.quantity) &&
    line.quantity > 0 &&
    typeof line.price === 'number' &&
    Number.isFinite(line.price) &&
    line.price >= 0
  )
}

/** Reads the saved cart. Anything broken or tampered with is dropped instead of crashing the app. */
export function parseStoredCart(raw) {
  if (!raw) return []
  try {
    const data = JSON.parse(raw)
    return Array.isArray(data) ? data.filter(isValidLine) : []
  } catch {
    return []
  }
}

/** Adds a variant, or raises the quantity of the line that already has it. */
export function addLine(lines, item, quantity, stock) {
  if (!Number.isInteger(quantity) || quantity <= 0 || !(stock > 0)) return lines
  const existing = lines.find((line) => line.variantId === item.variantId)
  if (existing) {
    return lines.map((line) =>
      line.variantId === item.variantId
        ? { ...line, maxStock: stock, quantity: capQuantity(line.quantity + quantity, stock) }
        : line,
    )
  }
  return [...lines, { ...item, maxStock: stock, quantity: capQuantity(quantity, stock) }]
}

/** Sets a line's quantity. Zero or less removes the line; above the stock is capped at the stock. */
export function setLineQuantity(lines, variantId, quantity) {
  if (!Number.isFinite(quantity)) return lines
  const whole = Math.floor(quantity)
  if (whole <= 0) return removeLine(lines, variantId)
  return lines.map((line) =>
    line.variantId === variantId ? { ...line, quantity: capQuantity(whole, line.maxStock) } : line,
  )
}

export function removeLine(lines, variantId) {
  return lines.filter((line) => line.variantId !== variantId)
}

/** Number of items (not lines): two shirts and one jean is 3. */
export function countItems(lines) {
  return lines.reduce((total, line) => total + line.quantity, 0)
}

/** Unit price × quantity, rounded to cents so 0.1 + 0.2 style errors never show. */
export function lineTotal(line) {
  return toCents(line.price * line.quantity) / 100
}

export function subtotal(lines) {
  return lines.reduce((cents, line) => cents + toCents(lineTotal(line)), 0) / 100
}
