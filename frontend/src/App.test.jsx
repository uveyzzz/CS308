import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'

function renderAt(path) {
  render(
    <MemoryRouter initialEntries={[path]}>
      <App />
    </MemoryRouter>,
  )
}

describe('App routing', () => {
  beforeEach(() => {
    // A Response body can be read only once, so every request gets a new one.
    // The header asks for the category menu, the home page for the backend status.
    vi.spyOn(globalThis, 'fetch').mockImplementation(async (path) =>
      new Response(JSON.stringify(path === '/api/categories' ? [] : { status: 'UP' })),
    )
  })

  it('renders the home page at /', async () => {
    renderAt('/')
    expect(screen.getByRole('heading', { name: 'Welcome' })).toBeInTheDocument()
    await screen.findByText('Backend is up')
  })

  it('renders the 404 page for unknown paths', () => {
    renderAt('/does-not-exist')
    expect(screen.getByRole('heading', { name: 'Page not found' })).toBeInTheDocument()
  })
})
