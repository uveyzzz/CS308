import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { setToken } from '../api/client'
import { USER, mockApi, renderApp } from '../test/renderApp'

const SHIRT = {
  variantId: 11, productId: 1, slug: 'oxford-shirt', name: 'Oxford Shirt',
  size: 'M', color: 'Blue', price: 39.9, image: '/shirt.jpg', quantity: 2, maxStock: 3,
}
const JEAN = {
  variantId: 21, productId: 2, slug: 'straight-jean', name: 'Straight Jean',
  size: '32', color: 'Indigo', price: 59.99, image: '/jean.jpg', quantity: 1, maxStock: 4,
}

/** Opens /cart with these lines already saved, as if the guest had added them before. */
function openCart(lines = [SHIRT, JEAN]) {
  localStorage.setItem('cart', JSON.stringify(lines))
  mockApi()
  renderApp('/cart')
}

const shirtLabel = 'Oxford Shirt (M / Blue)'

describe('CartPage', () => {
  it('shows each line with image, name, size, color, unit price, quantity and line total', () => {
    openCart()
    const line = screen.getAllByRole('listitem')[0]
    expect(within(line).getByRole('img', { name: 'Oxford Shirt' })).toHaveAttribute('src', '/shirt.jpg')
    expect(within(line).getByRole('link', { name: 'Oxford Shirt' })).toHaveAttribute('href', '/product/oxford-shirt')
    expect(within(line).getByText('Size: M · Color: Blue')).toBeInTheDocument()
    expect(within(line).getByText('$39.90 each')).toBeInTheDocument()
    expect(screen.getByLabelText(`Quantity of ${shirtLabel}`)).toHaveTextContent('2')
    expect(screen.getByLabelText(`Total for ${shirtLabel}`)).toHaveTextContent('$79.80')
  })

  it('shows the subtotal of all lines', () => {
    openCart()
    expect(screen.getByLabelText('Subtotal')).toHaveTextContent('$139.79')
    expect(screen.getByText('Subtotal (3 items)')).toBeInTheDocument()
  })

  it('updates the line total, subtotal and header count when the quantity changes', async () => {
    openCart()
    await userEvent.click(screen.getByRole('button', { name: `Increase quantity of ${shirtLabel}` }))

    expect(screen.getByLabelText(`Quantity of ${shirtLabel}`)).toHaveTextContent('3')
    expect(screen.getByLabelText(`Total for ${shirtLabel}`)).toHaveTextContent('$119.70')
    expect(screen.getByLabelText('Subtotal')).toHaveTextContent('$179.69')
    expect(screen.getByRole('link', { name: 'Cart (4)' })).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: `Decrease quantity of ${shirtLabel}` }))
    expect(screen.getByLabelText('Subtotal')).toHaveTextContent('$139.79')
  })

  it('does not let the quantity go above the stock', () => {
    openCart([{ ...SHIRT, quantity: 3 }])
    expect(screen.getByRole('button', { name: `Increase quantity of ${shirtLabel}` })).toBeDisabled()
  })

  it('does not let the quantity go below one (Remove is used for that)', () => {
    openCart([{ ...SHIRT, quantity: 1 }])
    expect(screen.getByRole('button', { name: `Decrease quantity of ${shirtLabel}` })).toBeDisabled()
  })

  it('removes a line and saves the change', async () => {
    openCart()
    await userEvent.click(screen.getByRole('button', { name: `Remove ${shirtLabel}` }))

    expect(screen.queryByText('Oxford Shirt')).not.toBeInTheDocument()
    expect(screen.getByLabelText('Subtotal')).toHaveTextContent('$59.99')
    expect(JSON.parse(localStorage.getItem('cart')).map((l) => l.variantId)).toEqual([21])
  })

  it('shows the empty state when the last item is removed', async () => {
    openCart([JEAN])
    await userEvent.click(screen.getByRole('button', { name: 'Remove Straight Jean (32 / Indigo)' }))

    expect(screen.getByText('Your cart is empty.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Browse products' })).toHaveAttribute('href', '/products')
  })

  it('shows the empty state for a new visitor', () => {
    openCart([])
    expect(screen.getByText('Your cart is empty.')).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Proceed to checkout' })).not.toBeInTheDocument()
  })

  it('sends a guest to the login page when proceeding to checkout', async () => {
    openCart()
    expect(screen.getByText('You will need to log in to check out.')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('link', { name: 'Proceed to checkout' }))

    expect(await screen.findByRole('heading', { name: 'Log in' })).toBeInTheDocument()
  })

  it('brings the guest back to the cart after logging in, with the cart merged (SCRUM-24)', async () => {
    const fetchSpy = mockApi({
      'POST /api/auth/login': [200, { token: 'abc', user: USER }],
      'POST /api/cart/merge': [200, [SHIRT]],
    })
    localStorage.setItem('cart', JSON.stringify([SHIRT]))
    renderApp('/cart')

    await userEvent.click(screen.getByRole('link', { name: 'Proceed to checkout' }))
    await userEvent.type(await screen.findByLabelText('Email'), 'ada@example.com')
    await userEvent.type(screen.getByLabelText('Password'), 'long-enough')
    await userEvent.click(screen.getByRole('button', { name: 'Log in' }))

    expect(await screen.findByRole('heading', { name: 'Your cart' })).toBeInTheDocument()
    expect(await screen.findByLabelText(`Quantity of ${shirtLabel}`)).toHaveTextContent('2')
    const merge = fetchSpy.mock.calls.find(([path]) => path === '/api/cart/merge')
    expect(JSON.parse(merge[1].body)).toEqual({ items: [{ variantId: 11, quantity: 2 }] })
  })

  it('lets a logged-in customer go to checkout', async () => {
    setToken('abc')
    localStorage.setItem('cart', JSON.stringify([SHIRT]))
    mockApi({ 'GET /api/me': [200, USER], 'POST /api/cart/merge': [200, [SHIRT]] })
    renderApp('/cart')

    await userEvent.click(await screen.findByRole('link', { name: 'Proceed to checkout' }))
    expect(await screen.findByRole('heading', { name: 'Checkout' })).toBeInTheDocument()
  })

  it('opens from the Cart link in the header', async () => {
    localStorage.setItem('cart', JSON.stringify([SHIRT]))
    mockApi({ 'GET /api/health': [200, { status: 'UP' }] })
    renderApp('/')

    await userEvent.click(screen.getByRole('link', { name: 'Cart (2)' }))
    expect(screen.getByRole('heading', { name: 'Your cart' })).toBeInTheDocument()
  })
})
