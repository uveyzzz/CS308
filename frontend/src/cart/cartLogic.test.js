import { describe, expect, it } from 'vitest'
import {
  addLine,
  countItems,
  lineTotal,
  parseStoredCart,
  removeLine,
  setLineQuantity,
  subtotal,
} from './cartLogic'

const SHIRT_M = { variantId: 11, productId: 1, slug: 'oxford-shirt', name: 'Oxford Shirt', size: 'M', color: 'Blue', price: 39.9, image: '/shirt.jpg' }
const SHIRT_L = { ...SHIRT_M, variantId: 12, size: 'L' }
const JEAN = { variantId: 21, productId: 2, slug: 'straight-jean', name: 'Straight Jean', size: '32', color: 'Indigo', price: 59.99, image: '/jean.jpg' }

describe('addLine', () => {
  it('adds a new variant as its own line and remembers the stock', () => {
    const lines = addLine([], SHIRT_M, 2, 5)
    expect(lines).toEqual([{ ...SHIRT_M, quantity: 2, maxStock: 5 }])
  })

  it('raises the quantity when the same variant is added again', () => {
    const lines = addLine(addLine([], SHIRT_M, 2, 5), SHIRT_M, 1, 5)
    expect(lines).toHaveLength(1)
    expect(lines[0].quantity).toBe(3)
  })

  it('keeps different sizes of the same product as separate lines', () => {
    const lines = addLine(addLine([], SHIRT_M, 1, 5), SHIRT_L, 1, 5)
    expect(lines.map((l) => l.variantId)).toEqual([11, 12])
  })

  it('never goes above the stock', () => {
    const lines = addLine(addLine([], SHIRT_M, 4, 5), SHIRT_M, 4, 5)
    expect(lines[0].quantity).toBe(5)
  })

  it('ignores out-of-stock variants and non-positive quantities', () => {
    expect(addLine([], SHIRT_M, 1, 0)).toEqual([])
    expect(addLine([], SHIRT_M, 0, 5)).toEqual([])
    expect(addLine([], SHIRT_M, -2, 5)).toEqual([])
  })

  it('does not change the lines it was given', () => {
    const before = addLine([], SHIRT_M, 1, 5)
    const snapshot = structuredClone(before)
    addLine(before, SHIRT_M, 1, 5)
    expect(before).toEqual(snapshot)
  })
})

describe('setLineQuantity', () => {
  const lines = addLine(addLine([], SHIRT_M, 2, 5), JEAN, 1, 3)

  it('sets the quantity of one line only', () => {
    const updated = setLineQuantity(lines, 11, 4)
    expect(updated.map((l) => l.quantity)).toEqual([4, 1])
  })

  it('caps the quantity at the stock', () => {
    expect(setLineQuantity(lines, 21, 10)[1].quantity).toBe(3)
  })

  it('removes the line when the quantity drops to zero', () => {
    expect(setLineQuantity(lines, 11, 0).map((l) => l.variantId)).toEqual([21])
  })

  it('ignores values that are not numbers', () => {
    expect(setLineQuantity(lines, 11, Number.NaN)).toBe(lines)
  })
})

describe('removeLine', () => {
  it('removes only the given variant', () => {
    const lines = addLine(addLine([], SHIRT_M, 1, 5), SHIRT_L, 1, 5)
    expect(removeLine(lines, 11).map((l) => l.variantId)).toEqual([12])
  })
})

describe('totals', () => {
  const lines = addLine(addLine([], SHIRT_M, 3, 5), JEAN, 2, 3)

  it('counts items, not lines', () => {
    expect(countItems(lines)).toBe(5)
    expect(countItems([])).toBe(0)
  })

  it('multiplies unit price by quantity for a line total', () => {
    expect(lineTotal(lines[0])).toBe(119.7)
    expect(lineTotal(lines[1])).toBe(119.98)
  })

  it('adds line totals to the subtotal without floating point errors', () => {
    expect(subtotal(lines)).toBe(239.68)
    expect(subtotal(addLine([], { ...JEAN, price: 0.1 }, 3, 5))).toBe(0.3)
    expect(subtotal([])).toBe(0)
  })
})

describe('parseStoredCart', () => {
  it('reads a saved cart back', () => {
    const lines = addLine([], SHIRT_M, 2, 5)
    expect(parseStoredCart(JSON.stringify(lines))).toEqual(lines)
  })

  it('returns an empty cart for missing or broken data', () => {
    expect(parseStoredCart(null)).toEqual([])
    expect(parseStoredCart('not json')).toEqual([])
    expect(parseStoredCart('{"a":1}')).toEqual([])
  })

  it('drops lines with a missing variant, a bad quantity or a bad price', () => {
    const raw = JSON.stringify([
      { ...SHIRT_M, quantity: 1 },
      { ...SHIRT_L, quantity: 0 },
      { ...JEAN, quantity: 1.5 },
      { ...JEAN, variantId: undefined, quantity: 1 },
      { ...JEAN, price: 'free', quantity: 1 },
    ])
    expect(parseStoredCart(raw).map((l) => l.variantId)).toEqual([11])
  })
})
