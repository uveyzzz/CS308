import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { mockApi, renderApp } from '../test/renderApp'

const item = (id, over = {}) => ({
  id, slug: `p-${id}`, name: `Product ${id}`, price: 50, discountedPrice: null, rating: 4, stock: 10, imageUrl: '/x.jpg', ...over,
})

const PAGE_1 = { content: [item(1), item(2, { discountedPrice: 40 }), item(3, { stock: 0 })], totalPages: 2 }
const PAGE_2 = { content: [item(4)], totalPages: 2 }

describe('ProductListPage', () => {
  it('shows loading skeletons before the products arrive', async () => {
    mockApi({ 'GET /api/products?page=0&size=12': [200, PAGE_1] })
    renderApp('/products')
    expect(screen.getAllByTestId('product-skeleton')).toHaveLength(12)
    await screen.findByText('Product 1')
    expect(screen.queryByTestId('product-skeleton')).not.toBeInTheDocument()
  })

  it('renders a card for each product', async () => {
    mockApi({ 'GET /api/products?page=0&size=12': [200, PAGE_1] })
    renderApp('/products')
    expect(await screen.findByRole('link', { name: 'Product 1' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Product 2' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Product 3' })).toBeInTheDocument()
  })

  it('shows the discounted price and the stock badge on cards', async () => {
    mockApi({ 'GET /api/products?page=0&size=12': [200, PAGE_1] })
    renderApp('/products')
    expect(await screen.findByText('$40.00')).toBeInTheDocument()
    expect(screen.getByText('Out of stock')).toBeInTheDocument()
  })

  it('only requests the product list, never product details', async () => {
    const fetchMock = mockApi({ 'GET /api/products?page=0&size=12': [200, PAGE_1] })
    renderApp('/products')
    await screen.findByText('Product 1')
    const urls = fetchMock.mock.calls.map(([path]) => path)
    expect(urls.filter((u) => u.startsWith('/api/products'))).toEqual(['/api/products?page=0&size=12'])
    expect(urls.some((u) => u.includes('details'))).toBe(false)
  })

  it('loads the next page when Next is clicked', async () => {
    mockApi({
      'GET /api/products?page=0&size=12': [200, PAGE_1],
      'GET /api/products?page=1&size=12': [200, PAGE_2],
    })
    renderApp('/products')
    await screen.findByText('Product 1')
    expect(screen.getByText('Page 1 of 2')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Next' }))
    expect(await screen.findByText('Product 4')).toBeInTheDocument()
    expect(screen.getByText('Page 2 of 2')).toBeInTheDocument()
  })

  it('opens directly on the page in the URL', async () => {
    mockApi({ 'GET /api/products?page=1&size=12': [200, PAGE_2] })
    renderApp('/products?page=2')
    expect(await screen.findByText('Product 4')).toBeInTheDocument()
  })

  it('disables Previous on the first page and Next on the last', async () => {
    mockApi({ 'GET /api/products?page=0&size=12': [200, PAGE_1] })
    renderApp('/products')
    await screen.findByText('Product 1')
    expect(screen.getByRole('button', { name: 'Previous' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Next' })).toBeEnabled()
  })

  it('hides pagination when there is only one page', async () => {
    mockApi({ 'GET /api/products?page=0&size=12': [200, { content: [item(1)], totalPages: 1 }] })
    renderApp('/products')
    await screen.findByText('Product 1')
    expect(screen.queryByRole('navigation', { name: 'Pagination' })).not.toBeInTheDocument()
  })

  it('shows an error when the request fails', async () => {
    mockApi({ 'GET /api/products?page=0&size=12': [500, { message: 'Server error' }] })
    renderApp('/products')
    expect(await screen.findByRole('alert')).toHaveTextContent('Server error')
  })

  it('shows a message when there are no products', async () => {
    mockApi({ 'GET /api/products?page=0&size=12': [200, { content: [], totalPages: 0 }] })
    renderApp('/products')
    await waitFor(() => expect(screen.getByText('No products found.')).toBeInTheDocument())
  })
})
