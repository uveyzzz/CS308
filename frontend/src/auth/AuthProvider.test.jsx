import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { getToken, setToken } from '../api/client'
import { USER, mockApi, renderApp } from '../test/renderApp'

describe('session handling', () => {
  it('shows a guest the login links and makes no account request', async () => {
    const fetchSpy = mockApi()
    renderApp('/')

    expect(screen.getByRole('link', { name: 'Log in' })).toBeInTheDocument()
    await screen.findByText('Backend is up')
    expect(fetchSpy.mock.calls.map(([path]) => path)).not.toContain('/api/me')
  })

  it('keeps the user logged in after a page refresh', async () => {
    setToken('abc')
    mockApi({ 'GET /api/me': [200, USER] })
    renderApp('/')

    expect(await screen.findByRole('button', { name: 'Log out' })).toBeInTheDocument()
  })

  it('logs out by forgetting the token', async () => {
    setToken('abc')
    mockApi({ 'GET /api/me': [200, USER] })
    const user = userEvent.setup()
    renderApp('/')

    await user.click(await screen.findByRole('button', { name: 'Log out' }))

    expect(screen.getByRole('link', { name: 'Log in' })).toBeInTheDocument()
    expect(getToken()).toBeNull()
  })

  it('drops a token the backend rejects', async () => {
    setToken('expired')
    mockApi({ 'GET /api/me': [401] })
    renderApp('/')

    expect(await screen.findByRole('link', { name: 'Log in' })).toBeInTheDocument()
    await waitFor(() => expect(getToken()).toBeNull())
  })

  it('keeps the token when the backend is only unreachable', async () => {
    setToken('abc')
    mockApi({ 'GET /api/me': [500] })
    renderApp('/')

    expect(await screen.findByRole('link', { name: 'Log in' })).toBeInTheDocument()
    expect(getToken()).toBe('abc')
  })
})
