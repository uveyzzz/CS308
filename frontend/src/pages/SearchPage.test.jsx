import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { mockApi, renderApp } from '../test/renderApp'

const item = (id, over = {}) => ({
  id, slug: `p-${id}`, name: `Product ${id}`, price: 50, discountedPrice: null, rating: 4, stock: 10, imageUrl: '/x.jpg', ...over,
})

// encodeURIComponent('gömlek'): the browser sends Turkish letters percent-encoded.
const SEARCH = 'GET /api/products?page=0&size=12&q=g%C3%B6mlek'

describe('SearchPage', () => {
  it('shows the products that match the search', async () => {
    mockApi({ [SEARCH]: [200, { content: [item(1), item(2)], totalPages: 1 }] })
    renderApp('/search?q=gömlek')
    expect(await screen.findByRole('heading', { name: /results for "gömlek"/i })).toBeInTheDocument()
    expect(await screen.findByRole('link', { name: 'Product 1' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Product 2' })).toBeInTheDocument()
  })

  it('lists out-of-stock products too, with their badge', async () => {
    mockApi({ [SEARCH]: [200, { content: [item(3, { stock: 0 })], totalPages: 1 }] })
    renderApp('/search?q=gömlek')
    expect(await screen.findByRole('link', { name: 'Product 3' })).toBeInTheDocument()
    expect(screen.getByText('Out of stock')).toBeInTheDocument()
  })

  it('says so when nothing matches', async () => {
    mockApi({ [SEARCH]: [200, { content: [], totalPages: 0 }] })
    renderApp('/search?q=gömlek')
    expect(await screen.findByText('No products match "gömlek".')).toBeInTheDocument()
  })

  it('sorts the results and keeps the search', async () => {
    mockApi({
      [SEARCH]: [200, { content: [item(1)], totalPages: 1 }],
      'GET /api/products?page=0&size=12&sort=price_asc&q=g%C3%B6mlek': [200, { content: [item(8)], totalPages: 1 }],
    })
    renderApp('/search?q=gömlek')
    await screen.findByRole('link', { name: 'Product 1' })
    await userEvent.selectOptions(screen.getByLabelText('Sort by'), 'price_asc')
    expect(await screen.findByRole('link', { name: 'Product 8' })).toBeInTheDocument()
  })

  it('asks for a search and sends no request when the search is empty', async () => {
    const fetchMock = mockApi()
    renderApp('/search')
    expect(await screen.findByRole('heading', { name: 'Search' })).toBeInTheDocument()
    const urls = fetchMock.mock.calls.map(([path]) => path)
    expect(urls.some((u) => u.startsWith('/api/products?page=0&size=12'))).toBe(false)
  })

  it('opens the results page from the search box in the header', async () => {
    mockApi({ [SEARCH]: [200, { content: [item(1)], totalPages: 1 }] })
    renderApp('/')
    await userEvent.type(screen.getByRole('searchbox', { name: 'Search products' }), 'gömlek')
    await userEvent.click(screen.getByRole('button', { name: 'Search' }))
    expect(await screen.findByRole('heading', { name: /results for "gömlek"/i })).toBeInTheDocument()
    expect(await screen.findByRole('link', { name: 'Product 1' })).toBeInTheDocument()
  })

  it('ignores an empty search box', async () => {
    mockApi()
    renderApp('/')
    await userEvent.type(screen.getByRole('searchbox', { name: 'Search products' }), '   ')
    await userEvent.click(screen.getByRole('button', { name: 'Search' }))
    expect(screen.queryByRole('heading', { name: 'Search' })).not.toBeInTheDocument()
  })
})
