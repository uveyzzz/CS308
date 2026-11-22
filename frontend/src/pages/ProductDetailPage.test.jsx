import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { mockApi, renderApp } from '../test/renderApp'

const PRODUCT = {
  id: 7, slug: 'oud-amber', name: 'Oud & Amber', price: 120, discountedPrice: 100,
  images: ['/one.jpg', '/two.jpg'],
  description: 'Warm and woody.',
  model: 'NM-007', serialNumber: 'SN-12345', warrantyStatus: '2 years', distributor: 'Perfumerie Ltd',
  variants: [
    { id: 71, size: '30ml', color: 'Amber', stock: 8 },
    { id: 72, size: '30ml', color: 'Clear', stock: 0 },
    { id: 73, size: '50ml', color: 'Amber', stock: 3 },
    { id: 74, size: '50ml', color: 'Clear', stock: 12 },
  ],
}

const open = (product = PRODUCT) => {
  const fetchMock = mockApi({ [`GET /api/products/${product.slug}`]: [200, product] })
  renderApp(`/product/${product.slug}`)
  return fetchMock
}

describe('ProductDetailPage', () => {
  it('shows name, prices and description', async () => {
    open()
    expect(await screen.findByRole('heading', { name: 'Oud & Amber' })).toBeInTheDocument()
    expect(screen.getByText('$100.00')).toBeInTheDocument()
    expect(screen.getByLabelText('Original price')).toHaveTextContent('$120.00')
    expect(screen.getByText('Warm and woody.')).toBeInTheDocument()
  })

  it('shows model, serial number, warranty and distributor', async () => {
    open()
    await screen.findByRole('heading', { name: 'Oud & Amber' })
    expect(screen.getByText('NM-007')).toBeInTheDocument()
    expect(screen.getByText('SN-12345')).toBeInTheDocument()
    expect(screen.getByText('2 years')).toBeInTheDocument()
    expect(screen.getByText('Perfumerie Ltd')).toBeInTheDocument()
  })

  it('switches the main image from the gallery thumbnails', async () => {
    open()
    const main = await screen.findByRole('img', { name: 'Oud & Amber' })
    expect(main).toHaveAttribute('src', '/one.jpg')
    await userEvent.click(screen.getByRole('button', { name: 'Show image 2' }))
    expect(main).toHaveAttribute('src', '/two.jpg')
  })

  it('starts on the first variant that is in stock and shows its stock', async () => {
    open()
    expect(await screen.findByText('Stock: 8')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '30ml' })).toHaveAttribute('aria-pressed', 'true')
    expect(screen.getByRole('button', { name: 'Amber' })).toHaveAttribute('aria-pressed', 'true')
  })

  it('updates the stock when the size changes', async () => {
    open()
    await screen.findByText('Stock: 8')
    await userEvent.click(screen.getByRole('button', { name: '50ml' }))
    expect(screen.getByText('Stock: 3')).toBeInTheDocument()
  })

  it('updates the stock when the color changes', async () => {
    open()
    await screen.findByText('Stock: 8')
    await userEvent.click(screen.getByRole('button', { name: '50ml' }))
    await userEvent.click(screen.getByRole('button', { name: 'Clear' }))
    expect(screen.getByText('Stock: 12')).toBeInTheDocument()
  })

  it('disables an out-of-stock variant', async () => {
    open()
    await screen.findByText('Stock: 8')
    // 30ml + Clear has no stock, so Clear cannot be chosen while 30ml is selected.
    expect(screen.getByRole('button', { name: 'Clear' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Amber' })).toBeEnabled()
  })

  it('adds the selected variant and quantity to the cart', async () => {
    open()
    await screen.findByText('Stock: 8')
    await userEvent.click(screen.getByRole('button', { name: '50ml' }))
    await userEvent.click(screen.getByRole('button', { name: 'Clear' }))
    await userEvent.click(screen.getByRole('button', { name: 'Increase quantity' }))
    await userEvent.click(screen.getByRole('button', { name: 'Add to cart' }))

    expect(screen.getByRole('status')).toHaveTextContent('50ml / Clear × 2')
    expect(screen.getByText('Cart (2)')).toBeInTheDocument()
    const [line] = JSON.parse(localStorage.getItem('cart'))
    expect(line).toMatchObject({ variantId: 74, size: '50ml', color: 'Clear', quantity: 2, price: 100 })
  })

  it('keeps separate cart lines for different variants', async () => {
    open()
    await screen.findByText('Stock: 8')
    await userEvent.click(screen.getByRole('button', { name: 'Add to cart' }))
    await userEvent.click(screen.getByRole('button', { name: '50ml' }))
    await userEvent.click(screen.getByRole('button', { name: 'Add to cart' }))
    const ids = JSON.parse(localStorage.getItem('cart')).map((l) => l.variantId)
    expect(ids).toEqual([71, 73])
  })

  it('does not let the quantity go above the stock', async () => {
    open()
    await screen.findByText('Stock: 8')
    await userEvent.click(screen.getByRole('button', { name: '50ml' }))
    const plus = screen.getByRole('button', { name: 'Increase quantity' })
    await userEvent.click(plus)
    await userEvent.click(plus)
    expect(screen.getByLabelText('Quantity')).toHaveTextContent('3')
    expect(plus).toBeDisabled()
  })

  it('resets the quantity to 1 when the variant changes', async () => {
    open()
    await screen.findByText('Stock: 8')
    await userEvent.click(screen.getByRole('button', { name: 'Increase quantity' }))
    await userEvent.click(screen.getByRole('button', { name: '50ml' }))
    expect(screen.getByLabelText('Quantity')).toHaveTextContent('1')
  })

  it('disables Add to cart when every variant is out of stock', async () => {
    open({ ...PRODUCT, variants: PRODUCT.variants.map((v) => ({ ...v, stock: 0 })) })
    expect(await screen.findByText('Out of stock')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Add to cart' })).toBeDisabled()
  })

  it('shows a not found message for an unknown slug', async () => {
    mockApi({ 'GET /api/products/nope': [404, { message: 'Product not found' }] })
    renderApp('/product/nope')
    expect(await screen.findByText(/Product not found/)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Back to products' })).toHaveAttribute('href', '/products')
  })

  it('shows an error when loading fails', async () => {
    mockApi({ 'GET /api/products/oud-amber': [500, { message: 'Boom' }] })
    renderApp('/product/oud-amber')
    expect(await screen.findByRole('alert')).toHaveTextContent('Boom')
  })
})
