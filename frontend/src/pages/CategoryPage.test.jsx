import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { mockApi, renderApp } from '../test/renderApp'

const item = (id, over = {}) => ({
  id, slug: `p-${id}`, name: `Product ${id}`, price: 50, discountedPrice: 50, rating: 4, stock: 10, imageUrl: '/x.jpg', ...over,
})

const WOMEN = {
  id: 1, name: 'Women', slug: 'women', description: null,
  path: [{ name: 'Women', slug: 'women' }],
  children: [{ id: 3, name: 'Tops', slug: 'women-tops', children: [] }],
}

const WOMEN_TOPS = {
  id: 3, name: 'Tops', slug: 'women-tops', description: null,
  path: [{ name: 'Women', slug: 'women' }, { name: 'Tops', slug: 'women-tops' }],
  children: [],
}

const products = (content, totalPages = 1) => [200, { content, totalPages }]

describe('CategoryPage', () => {
  it('shows the category name and its products', async () => {
    mockApi({
      'GET /api/categories/women': [200, WOMEN],
      'GET /api/products?page=0&size=12&category=women': products([item(1), item(2)]),
    })
    renderApp('/category/women')
    expect(await screen.findByRole('heading', { name: 'Women' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Product 1' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Product 2' })).toBeInTheDocument()
  })

  it('shows a breadcrumb from Home down to the current category', async () => {
    mockApi({
      'GET /api/categories/women-tops': [200, WOMEN_TOPS],
      'GET /api/products?page=0&size=12&category=women-tops': products([item(1)]),
    })
    renderApp('/category/women-tops')
    const breadcrumb = await screen.findByRole('navigation', { name: 'Breadcrumb' })
    expect(within(breadcrumb).getByRole('link', { name: 'Home' })).toHaveAttribute('href', '/')
    expect(within(breadcrumb).getByRole('link', { name: 'Women' })).toHaveAttribute('href', '/category/women')
    // The current category is plain text, not a link to itself.
    expect(within(breadcrumb).queryByRole('link', { name: 'Tops' })).not.toBeInTheDocument()
    expect(within(breadcrumb).getByText('Tops')).toHaveAttribute('aria-current', 'page')
  })

  it('links to the subcategories', async () => {
    mockApi({
      'GET /api/categories/women': [200, WOMEN],
      'GET /api/products?page=0&size=12&category=women': products([item(1)]),
    })
    renderApp('/category/women')
    const links = await screen.findByRole('list', { name: 'Subcategories' })
    expect(within(links).getByRole('link', { name: 'Tops' })).toHaveAttribute('href', '/category/women-tops')
  })

  it('shows an empty state when the category has no products', async () => {
    mockApi({
      'GET /api/categories/women-tops': [200, WOMEN_TOPS],
      'GET /api/products?page=0&size=12&category=women-tops': products([], 0),
    })
    renderApp('/category/women-tops')
    expect(await screen.findByText(/No products in this category yet/)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'See all products' })).toHaveAttribute('href', '/products')
  })

  it('shows a not found page for an unknown category', async () => {
    mockApi({
      'GET /api/categories/kids': [404, { status: 404, message: 'Category not found' }],
      'GET /api/products?page=0&size=12&category=kids': [404, { status: 404, message: 'Category not found' }],
    })
    renderApp('/category/kids')
    expect(await screen.findByRole('heading', { name: 'Category not found' })).toBeInTheDocument()
  })

  it('keeps the category when moving to the next page', async () => {
    mockApi({
      'GET /api/categories/women': [200, WOMEN],
      'GET /api/products?page=0&size=12&category=women': products([item(1)], 2),
      'GET /api/products?page=1&size=12&category=women': products([item(13)], 2),
    })
    renderApp('/category/women')
    await screen.findByText('Product 1')
    await userEvent.click(screen.getByRole('button', { name: 'Next' }))
    expect(await screen.findByText('Product 13')).toBeInTheDocument()
    expect(screen.getByText('Page 2 of 2')).toBeInTheDocument()
  })

  it('loads the new category when the address changes', async () => {
    mockApi({
      'GET /api/categories/women': [200, WOMEN],
      'GET /api/products?page=0&size=12&category=women': products([item(1)]),
      'GET /api/categories/women-tops': [200, WOMEN_TOPS],
      'GET /api/products?page=0&size=12&category=women-tops': products([item(5)]),
    })
    renderApp('/category/women')
    const links = await screen.findByRole('list', { name: 'Subcategories' })
    await userEvent.click(within(links).getByRole('link', { name: 'Tops' }))
    expect(await screen.findByRole('heading', { name: 'Tops' })).toBeInTheDocument()
    expect(await screen.findByText('Product 5')).toBeInTheDocument()
    expect(screen.queryByText('Product 1')).not.toBeInTheDocument()
  })

  it('sorts the category products with the menu', async () => {
    mockApi({
      'GET /api/categories/women': [200, WOMEN],
      'GET /api/products?page=0&size=12&category=women': products([item(1)]),
      'GET /api/products?page=0&size=12&category=women&sort=price_desc': products([item(7)]),
    })
    renderApp('/category/women')
    await screen.findByText('Product 1')
    await userEvent.selectOptions(screen.getByLabelText('Sort by'), 'price_desc')
    expect(await screen.findByText('Product 7')).toBeInTheDocument()
  })
})
