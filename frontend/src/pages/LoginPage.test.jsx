import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { getToken, setToken } from '../api/client'
import { USER, mockApi, renderApp } from '../test/renderApp'

async function logIn(user) {
  await user.type(screen.getByLabelText('Email'), 'ada@example.com')
  await user.type(screen.getByLabelText('Password'), 'long-enough')
  await user.click(screen.getByRole('button', { name: 'Log in' }))
}

describe('LoginPage', () => {
  it('stores the token and goes home after a successful login', async () => {
    mockApi({ 'POST /api/auth/login': [200, { token: 'abc', user: USER }] })
    renderApp('/login')

    await logIn(userEvent.setup())

    await screen.findByRole('heading', { name: 'Welcome' })
    expect(getToken()).toBe('abc')
  })

  it('returns to the page the guest wanted to open', async () => {
    mockApi({ 'POST /api/auth/login': [200, { token: 'abc', user: USER }] })
    renderApp('/account/profile')

    await screen.findByRole('heading', { name: 'Log in' })
    await logIn(userEvent.setup())

    await screen.findByRole('heading', { name: 'My profile' })
  })

  it('shows the backend message and stays on the page when the login fails', async () => {
    mockApi({ 'POST /api/auth/login': [401, { status: 401, message: 'Invalid email or password' }] })
    renderApp('/login')

    await logIn(userEvent.setup())

    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid email or password')
    expect(screen.getByRole('heading', { name: 'Log in' })).toBeInTheDocument()
    expect(getToken()).toBeNull()
  })

  it('sends a user who is already logged in away from the page', async () => {
    setToken('abc')
    mockApi({ 'GET /api/me': [200, USER] })
    renderApp('/login')

    await screen.findByRole('heading', { name: 'Welcome' })
  })
})
