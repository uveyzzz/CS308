import { act, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import CartProvider from './CartProvider'
import { useCart } from './cartContext'

const SHIRT = { variantId: 11, productId: 1, slug: 'oxford-shirt', name: 'Oxford Shirt', size: 'M', color: 'Blue', price: 40 }

/** A tiny component that shows the cart and offers the cart actions as buttons. */
function CartProbe() {
  const { items, count, subtotal, addItem, updateQuantity, removeItem, clearCart } = useCart()
  return (
    <div>
      <p>count: {count}</p>
      <p>subtotal: {subtotal}</p>
      <p>lines: {items.map((l) => `${l.variantId}x${l.quantity}`).join(',')}</p>
      <button onClick={() => addItem(SHIRT, 2, 5)}>add</button>
      <button onClick={() => updateQuantity(11, 4)}>set 4</button>
      <button onClick={() => removeItem(11)}>remove</button>
      <button onClick={() => clearCart()}>clear</button>
    </div>
  )
}

const renderCart = () =>
  render(
    <CartProvider>
      <CartProbe />
    </CartProvider>,
  )

describe('CartProvider', () => {
  it('starts empty for a new visitor', () => {
    renderCart()
    expect(screen.getByText('count: 0')).toBeInTheDocument()
  })

  it('adds, updates and removes items and keeps the totals right', async () => {
    renderCart()
    await userEvent.click(screen.getByRole('button', { name: 'add' }))
    expect(screen.getByText('count: 2')).toBeInTheDocument()
    expect(screen.getByText('subtotal: 80')).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'set 4' }))
    expect(screen.getByText('lines: 11x4')).toBeInTheDocument()
    expect(screen.getByText('subtotal: 160')).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'remove' }))
    expect(screen.getByText('count: 0')).toBeInTheDocument()
  })

  it('saves the cart to localStorage as variant id and quantity', async () => {
    renderCart()
    await userEvent.click(screen.getByRole('button', { name: 'add' }))
    const [line] = JSON.parse(localStorage.getItem('cart'))
    expect(line).toMatchObject({ variantId: 11, quantity: 2 })
  })

  it('still has the cart after a page refresh', async () => {
    const first = renderCart()
    await userEvent.click(screen.getByRole('button', { name: 'add' }))
    first.unmount()

    renderCart()
    expect(screen.getByText('lines: 11x2')).toBeInTheDocument()
  })

  it('empties the cart and the saved copy with clearCart', async () => {
    renderCart()
    await userEvent.click(screen.getByRole('button', { name: 'add' }))
    await userEvent.click(screen.getByRole('button', { name: 'clear' }))
    expect(screen.getByText('count: 0')).toBeInTheDocument()
    expect(JSON.parse(localStorage.getItem('cart'))).toEqual([])
  })

  it('picks up a change made in another browser tab', () => {
    renderCart()
    const otherTab = JSON.stringify([{ ...SHIRT, quantity: 3, maxStock: 5 }])
    act(() => {
      window.dispatchEvent(new StorageEvent('storage', { key: 'cart', newValue: otherTab }))
    })
    expect(screen.getByText('count: 3')).toBeInTheDocument()
  })
})
