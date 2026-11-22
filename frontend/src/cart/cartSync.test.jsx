import { StrictMode } from 'react'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { AuthContext } from '../auth/authContext'
import CartProvider from './CartProvider'
import { useCart } from './cartContext'

// SCRUM-24: a logged-in customer's cart lives on the backend, and the guest cart is merged
// into it at login.

const CUSTOMER = { id: 1, role: 'CUSTOMER' }
const SHIRT = { variantId: 11, productId: 1, slug: 'oxford-shirt', name: 'Oxford Shirt', size: 'M', color: 'Blue', price: 40 }
const savedLine = (quantity) => ({ ...SHIRT, image: null, quantity, maxStock: 5 })

/** Fakes the backend; every request is recorded as "METHOD path" with its JSON body. */
function mockBackend(responses = {}) {
  const calls = []
  vi.spyOn(globalThis, 'fetch').mockImplementation(async (path, { method, body }) => {
    calls.push({ request: `${method} ${path}`, body: body ? JSON.parse(body) : undefined })
    const [status, data] = responses[`${method} ${path}`] ?? [200, []]
    return new Response(data === undefined ? null : JSON.stringify(data), { status })
  })
  return calls
}

function CartProbe() {
  const { items, count, addItem, updateQuantity, removeItem, clearCart } = useCart()
  return (
    <div>
      <p>count: {count}</p>
      <p>lines: {items.map((l) => `${l.variantId}x${l.quantity}`).join(',')}</p>
      <button onClick={() => addItem(SHIRT, 2, 5)}>add</button>
      <button onClick={() => updateQuantity(11, 4)}>set 4</button>
      <button onClick={() => updateQuantity(11, 0)}>set 0</button>
      <button onClick={() => removeItem(11)}>remove</button>
      <button onClick={() => clearCart()}>clear</button>
    </div>
  )
}

function renderCartFor(user) {
  const tree = (u) => (
    <StrictMode>
      <AuthContext.Provider value={{ user: u }}>
        <CartProvider>
          <CartProbe />
        </CartProvider>
      </AuthContext.Provider>
    </StrictMode>
  )
  const view = render(tree(user))
  return { ...view, logInAs: (u) => view.rerender(tree(u)) }
}

const guestCart = (lines) => localStorage.setItem('cart', JSON.stringify(lines))
const requests = (calls) => calls.map((c) => c.request)

describe('saved cart (SCRUM-24)', () => {
  it('merges the guest cart into the saved cart at login, once', async () => {
    guestCart([{ ...SHIRT, quantity: 2, maxStock: 5 }])
    const calls = mockBackend({ 'POST /api/cart/merge': [200, [savedLine(3)]] })
    const { logInAs } = renderCartFor(null)

    logInAs(CUSTOMER)

    expect(await screen.findByText('lines: 11x3')).toBeInTheDocument()
    const merges = calls.filter((c) => c.request === 'POST /api/cart/merge')
    expect(merges).toHaveLength(1)
    expect(merges[0].body).toEqual({ items: [{ variantId: 11, quantity: 2 }] })
  })

  it('clears the guest cart from the browser after a successful merge', async () => {
    guestCart([{ ...SHIRT, quantity: 2, maxStock: 5 }])
    mockBackend({ 'POST /api/cart/merge': [200, [savedLine(2)]] })
    renderCartFor(CUSTOMER)

    await screen.findByText('lines: 11x2')
    expect(localStorage.getItem('cart')).toBeNull()
  })

  it('keeps the guest cart in the browser when the merge fails', async () => {
    guestCart([{ ...SHIRT, quantity: 2, maxStock: 5 }])
    mockBackend({ 'POST /api/cart/merge': [500, { message: 'down' }] })
    renderCartFor(CUSTOMER)

    await waitFor(() => expect(globalThis.fetch).toHaveBeenCalled())
    expect(JSON.parse(localStorage.getItem('cart'))).toMatchObject([{ variantId: 11, quantity: 2 }])
  })

  it('loads the saved cart when there is nothing to merge', async () => {
    const calls = mockBackend({ 'GET /api/cart': [200, [savedLine(1)]] })
    renderCartFor(CUSTOMER)

    expect(await screen.findByText('lines: 11x1')).toBeInTheDocument()
    expect(requests(calls)).not.toContain('POST /api/cart/merge')
  })

  it('sends a customer’s changes to the backend', async () => {
    const calls = mockBackend({
      'POST /api/cart/items': [200, [savedLine(2)]],
      'PUT /api/cart/items/11': [200, [savedLine(4)]],
    })
    renderCartFor(CUSTOMER)
    await waitFor(() => expect(requests(calls)).toContain('GET /api/cart'))

    await userEvent.click(screen.getByRole('button', { name: 'add' }))
    expect(await screen.findByText('lines: 11x2')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'set 4' }))
    expect(await screen.findByText('lines: 11x4')).toBeInTheDocument()

    expect(calls.find((c) => c.request === 'POST /api/cart/items').body).toEqual({ variantId: 11, quantity: 2 })
    expect(calls.find((c) => c.request === 'PUT /api/cart/items/11').body).toEqual({ quantity: 4 })
  })

  it('removes a line when its quantity is set to zero, and can empty the cart', async () => {
    const calls = mockBackend({ 'GET /api/cart': [200, [savedLine(2)]], 'DELETE /api/cart': [204] })
    renderCartFor(CUSTOMER)
    await screen.findByText('lines: 11x2')

    await userEvent.click(screen.getByRole('button', { name: 'set 0' }))
    await userEvent.click(screen.getByRole('button', { name: 'clear' }))

    expect(await screen.findByText('count: 0')).toBeInTheDocument()
    expect(requests(calls)).toEqual(expect.arrayContaining(['DELETE /api/cart/items/11', 'DELETE /api/cart']))
  })

  it('shows an empty cart after logout; the saved cart stays on the backend', async () => {
    mockBackend({ 'GET /api/cart': [200, [savedLine(2)]] })
    const { logInAs } = renderCartFor(CUSTOMER)
    await screen.findByText('lines: 11x2')

    logInAs(null)

    expect(await screen.findByText('count: 0')).toBeInTheDocument()
  })

  it('keeps staff on the browser cart: they cannot buy, so nothing is sent', async () => {
    const calls = mockBackend()
    renderCartFor({ id: 2, role: 'SALES_MANAGER' })

    await userEvent.click(screen.getByRole('button', { name: 'add' }))

    expect(screen.getByText('lines: 11x2')).toBeInTheDocument()
    expect(calls).toHaveLength(0)
  })
})
