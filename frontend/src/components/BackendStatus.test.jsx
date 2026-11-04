import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import BackendStatus from './BackendStatus'

describe('BackendStatus', () => {
  it('shows up when the health check succeeds', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(JSON.stringify({ status: 'UP' })))
    render(<BackendStatus />)
    expect(await screen.findByText('Backend is up')).toBeInTheDocument()
  })

  it('shows unreachable when the request fails', async () => {
    vi.spyOn(globalThis, 'fetch').mockRejectedValue(new TypeError('Failed to fetch'))
    render(<BackendStatus />)
    expect(await screen.findByText('Backend is unreachable')).toBeInTheDocument()
  })
})
