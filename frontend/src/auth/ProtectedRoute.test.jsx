import { screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { setToken } from '../api/client'
import { USER, mockApi, renderApp } from '../test/renderApp'

describe('ProtectedRoute', () => {
  it('sends a guest to the login page', async () => {
    mockApi()
    renderApp('/account/profile')

    expect(await screen.findByRole('heading', { name: 'Log in' })).toBeInTheDocument()
  })

  it('lets a logged-in user through', async () => {
    setToken('abc')
    mockApi({ 'GET /api/me': [200, USER] })
    renderApp('/account/profile')

    expect(await screen.findByRole('heading', { name: 'My profile' })).toBeInTheDocument()
  })

  it('waits for the session check instead of redirecting too early', async () => {
    setToken('abc')
    mockApi({ 'GET /api/me': [200, USER] })
    renderApp('/account/profile')

    expect(screen.getByRole('status')).toHaveTextContent('Loading')
    await screen.findByRole('heading', { name: 'My profile' })
  })

  it('sends a user with an expired token to the login page', async () => {
    setToken('expired')
    mockApi({ 'GET /api/me': [401] })
    renderApp('/account/profile')

    expect(await screen.findByRole('heading', { name: 'Log in' })).toBeInTheDocument()
  })
})
