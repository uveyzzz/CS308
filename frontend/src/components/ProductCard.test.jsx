import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import ProductCard from './ProductCard'

const BASE = { id: 1, slug: 'oud-amber', name: 'Oud & Amber', price: 120, discountedPrice: null, rating: 4.5, stock: 20, imageUrl: '/a.jpg' }

function renderCard(overrides = {}) {
  return render(
    <MemoryRouter>
      <ProductCard product={{ ...BASE, ...overrides }} />
    </MemoryRouter>,
  )
}

describe('ProductCard', () => {
  it('shows the name, price and rating', () => {
    renderCard()
    expect(screen.getByRole('heading', { name: 'Oud & Amber' })).toBeInTheDocument()
    expect(screen.getByText('$120.00')).toBeInTheDocument()
    expect(screen.getByLabelText('Rating 4.5 out of 5')).toBeInTheDocument()
  })

  it('links to the product page by slug', () => {
    renderCard()
    expect(screen.getByRole('link', { name: 'Oud & Amber' })).toHaveAttribute('href', '/product/oud-amber')
  })

  it('shows the discounted price next to the original price', () => {
    renderCard({ discountedPrice: 99 })
    expect(screen.getByText('$99.00')).toBeInTheDocument()
    expect(screen.getByLabelText('Original price')).toHaveTextContent('$120.00')
  })

  it('shows no original price when there is no discount', () => {
    renderCard()
    expect(screen.queryByLabelText('Original price')).not.toBeInTheDocument()
  })

  it('shows an In stock badge when plenty is left', () => {
    renderCard({ stock: 20 })
    expect(screen.getByText('In stock')).toBeInTheDocument()
  })

  it('shows a low stock badge with the number left', () => {
    renderCard({ stock: 3 })
    expect(screen.getByText('Only 3 left')).toBeInTheDocument()
  })

  it('shows an Out of stock badge at zero stock', () => {
    renderCard({ stock: 0 })
    expect(screen.getByText('Out of stock')).toBeInTheDocument()
  })
})
