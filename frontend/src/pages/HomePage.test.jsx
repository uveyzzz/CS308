import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { mockApi, renderApp } from '../test/renderApp'

const TREE = [
  { id: 1, name: 'Women', slug: 'women', children: [{ id: 3, name: 'Tops', slug: 'women-tops', children: [] }] },
  { id: 2, name: 'Men', slug: 'men', children: [] },
]

const product = (id, name) => ({
  id, slug: `product-${id}`, name, price: 40, discountedPrice: 40, rating: 4, ratingCount: 1, stock: 5, imageUrl: null,
})

const NEWEST = [product(6, 'Denim Jacket'), product(5, 'Kids Hoodie'), product(4, 'Flannel Shirt'), product(3, 'Oxford Shirt')]

describe('HomePage (SCRUM-28)', () => {
  it('opens with a hero that leads to the catalog', async () => {
    mockApi()
    renderApp('/')

    expect(screen.getByRole('heading', { level: 1, name: 'Welcome' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Shop all products' })).toHaveAttribute('href', '/products')
  })

  it('features the top-level categories', async () => {
    mockApi({ 'GET /api/categories': [200, TREE] })
    renderApp('/')

    const section = await screen.findByRole('region', { name: 'Shop by category' })
    const links = within(section).getAllByRole('link')
    expect(links.map((link) => link.textContent)).toEqual(['Women', 'Men'])
    expect(links[0]).toHaveAttribute('href', '/category/women')
  })

  it('shows the four newest products as new arrivals', async () => {
    mockApi({ 'GET /api/products?page=0&size=4&sort=newest': [200, { content: NEWEST, totalPages: 2 }] })
    renderApp('/')

    const section = screen.getByRole('region', { name: 'New arrivals' })
    expect(await within(section).findByRole('link', { name: 'Denim Jacket' })).toHaveAttribute('href', '/product/product-6')
    expect(within(section).getAllByRole('link', { name: /Shirt|Hoodie|Jacket/ })).toHaveLength(4)
    expect(within(section).getByRole('link', { name: 'See all' })).toHaveAttribute('href', '/products')
  })

  it('shows the four best sellers, using the popularity sort from SCRUM-20', async () => {
    const popular = [product(2, 'Ribbed Tee'), product(3, 'Oxford Shirt'), product(5, 'Kids Hoodie'), product(1, 'Linen Blouse')]
    mockApi({ 'GET /api/products?page=0&size=4&sort=popular': [200, { content: popular, totalPages: 2 }] })
    renderApp('/')

    const section = screen.getByRole('region', { name: 'Best sellers' })
    await within(section).findByRole('link', { name: 'Ribbed Tee' })
    const names = within(section).getAllByRole('heading', { level: 3 }).map((h) => h.textContent)
    expect(names).toEqual(['Ribbed Tee', 'Oxford Shirt', 'Kids Hoodie', 'Linen Blouse'])
    expect(within(section).getByRole('link', { name: 'See all' })).toHaveAttribute('href', '/products?sort=popular')
  })

  it('shows placeholders while the product rows load', () => {
    mockApi()
    renderApp('/')

    expect(screen.getAllByTestId('product-skeleton')).toHaveLength(8)
  })

  it('leaves out sections that have nothing to show, and keeps the rest of the page', async () => {
    mockApi({
      'GET /api/categories': [500, {}],
      'GET /api/products?page=0&size=4&sort=newest': [500, {}],
      'GET /api/products?page=0&size=4&sort=popular': [200, { content: [], totalPages: 0 }],
    })
    renderApp('/')

    await screen.findByText('Backend is up')
    await waitFor(() => expect(screen.queryAllByTestId('product-skeleton')).toHaveLength(0))
    expect(screen.queryByRole('region', { name: 'Shop by category' })).not.toBeInTheDocument()
    expect(screen.queryByRole('region', { name: 'New arrivals' })).not.toBeInTheDocument()
    expect(screen.queryByRole('region', { name: 'Best sellers' })).not.toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Welcome' })).toBeInTheDocument()
  })
})

describe('header menu on small screens (SCRUM-28)', () => {
  it('opens and closes with the Menu button', async () => {
    mockApi()
    renderApp('/')
    const button = screen.getByRole('button', { name: 'Menu' })

    expect(button).toHaveAttribute('aria-expanded', 'false')
    await userEvent.click(button)
    expect(button).toHaveAttribute('aria-expanded', 'true')
    expect(document.getElementById('site-menu')).toHaveClass('header-menu-open')
    await userEvent.click(button)
    expect(button).toHaveAttribute('aria-expanded', 'false')
  })

  it('closes after following a link', async () => {
    mockApi({ 'GET /api/products?page=0&size=12': [200, { content: [], totalPages: 0 }] })
    renderApp('/')
    const button = screen.getByRole('button', { name: 'Menu' })

    await userEvent.click(button)
    await userEvent.click(within(document.getElementById('site-menu')).getByRole('link', { name: 'Products' }))

    expect(button).toHaveAttribute('aria-expanded', 'false')
  })
})
