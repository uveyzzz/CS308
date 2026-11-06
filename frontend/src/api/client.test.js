import { describe, expect, it, vi } from 'vitest'
import { ApiError, apiFetch, setToken } from './client'

function mockFetch(status, body) {
  return vi.spyOn(globalThis, 'fetch').mockResolvedValue(
    new Response(body === undefined ? null : JSON.stringify(body), { status }),
  )
}

describe('apiFetch', () => {
  it('returns parsed JSON on success', async () => {
    mockFetch(200, { status: 'UP' })
    await expect(apiFetch('/api/health')).resolves.toEqual({ status: 'UP' })
  })

  it('returns null for an empty body', async () => {
    mockFetch(204)
    await expect(apiFetch('/api/cart', { method: 'DELETE' })).resolves.toBeNull()
  })

  it('sends a JSON body with the right content type', async () => {
    const fetchSpy = mockFetch(201, { id: 1 })
    await apiFetch('/api/cart', { method: 'POST', body: { productId: 7 } })

    const [, options] = fetchSpy.mock.calls[0]
    expect(options.method).toBe('POST')
    expect(options.headers['Content-Type']).toBe('application/json')
    expect(options.body).toBe(JSON.stringify({ productId: 7 }))
  })

  it('attaches the bearer token when logged in', async () => {
    const fetchSpy = mockFetch(200, {})
    setToken('abc')
    await apiFetch('/api/orders')
    expect(fetchSpy.mock.calls[0][1].headers.Authorization).toBe('Bearer abc')
  })

  it('sends no Authorization header for guests', async () => {
    const fetchSpy = mockFetch(200, {})
    await apiFetch('/api/products')
    expect(fetchSpy.mock.calls[0][1].headers.Authorization).toBeUndefined()
  })

  it('throws ApiError with the backend message and field errors', async () => {
    mockFetch(400, { status: 400, message: 'Validation failed', fieldErrors: { email: 'must not be blank' } })

    const error = await apiFetch('/api/auth/register', { method: 'POST', body: {} }).catch((e) => e)
    expect(error).toBeInstanceOf(ApiError)
    expect(error.status).toBe(400)
    expect(error.message).toBe('Validation failed')
    expect(error.fieldErrors).toEqual({ email: 'must not be blank' })
  })

  it('falls back to a generic message when the error body is empty', async () => {
    mockFetch(500)
    await expect(apiFetch('/api/health')).rejects.toThrow('Request failed (500)')
  })
})
