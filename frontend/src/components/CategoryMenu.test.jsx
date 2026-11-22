import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { mockApi, renderApp } from '../test/renderApp'

const TREE = [
  { id: 1, name: 'Women', slug: 'women', children: [
    { id: 3, name: 'Tops', slug: 'women-tops', children: [] },
    { id: 4, name: 'Shoes', slug: 'women-shoes', children: [] },
  ] },
  { id: 2, name: 'Men', slug: 'men', children: [] },
]

describe('CategoryMenu', () => {
  it('shows every top-level category as a link to its page', async () => {
    mockApi({ 'GET /api/categories': [200, TREE] })
    renderApp('/')
    const menu = await screen.findByRole('navigation', { name: 'Categories' })
    expect(within(menu).getByRole('link', { name: 'Women' })).toHaveAttribute('href', '/category/women')
    expect(within(menu).getByRole('link', { name: 'Men' })).toHaveAttribute('href', '/category/men')
  })

  it('lists the subcategories under their parent', async () => {
    mockApi({ 'GET /api/categories': [200, TREE] })
    renderApp('/')
    const submenu = await screen.findByRole('list', { name: 'Women subcategories' })
    expect(within(submenu).getAllByRole('link').map((a) => a.textContent)).toEqual(['Tops', 'Shoes'])
    expect(within(submenu).getByRole('link', { name: 'Tops' })).toHaveAttribute('href', '/category/women-tops')
  })

  it('has no subcategory list for a category without children', async () => {
    mockApi({ 'GET /api/categories': [200, TREE] })
    renderApp('/')
    await screen.findByRole('navigation', { name: 'Categories' })
    expect(screen.queryByRole('list', { name: 'Men subcategories' })).not.toBeInTheDocument()
  })

  it('opens the category page when a subcategory is clicked', async () => {
    mockApi({
      'GET /api/categories': [200, TREE],
      'GET /api/categories/women-tops': [200, {
        id: 3, name: 'Tops', slug: 'women-tops', description: null,
        path: [{ name: 'Women', slug: 'women' }, { name: 'Tops', slug: 'women-tops' }], children: [],
      }],
      'GET /api/products?page=0&size=12&category=women-tops': [200, { content: [], totalPages: 0 }],
    })
    renderApp('/')
    const submenu = await screen.findByRole('list', { name: 'Women subcategories' })
    await userEvent.click(within(submenu).getByRole('link', { name: 'Tops' }))
    expect(await screen.findByRole('heading', { name: 'Tops' })).toBeInTheDocument()
  })

  it('leaves the menu out but keeps the header when categories fail to load', async () => {
    mockApi({ 'GET /api/categories': [500, { message: 'Server error' }] })
    renderApp('/')
    expect(await screen.findByRole('link', { name: 'CS308 Store' })).toBeInTheDocument()
    expect(screen.queryByRole('navigation', { name: 'Categories' })).not.toBeInTheDocument()
  })
})
