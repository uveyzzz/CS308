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
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(JSON.stringify({ status: 'UP' })))
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
