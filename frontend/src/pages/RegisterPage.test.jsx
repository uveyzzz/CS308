import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { getToken } from '../api/client'
import { USER, mockApi, renderApp } from '../test/renderApp'

async function fillIn(user) {
  await user.type(screen.getByLabelText('Full name'), 'Ada Lovelace')
  await user.type(screen.getByLabelText('Email'), 'ada@example.com')
  await user.type(screen.getByLabelText('Password'), 'long-enough')
  await user.click(screen.getByRole('button', { name: 'Create account' }))
}

describe('RegisterPage', () => {
  it('logs the new user in and goes home', async () => {
    mockApi({ 'POST /api/auth/register': [201, { token: 'abc', user: USER }] })
    renderApp('/register')

    await fillIn(userEvent.setup())

    await screen.findByRole('heading', { name: 'Welcome' })
    expect(getToken()).toBe('abc')
  })

  it('never sends a role, and leaves out an empty address', async () => {
    const fetchSpy = mockApi({ 'POST /api/auth/register': [201, { token: 'abc', user: USER }] })
    renderApp('/register')

    await fillIn(userEvent.setup())
    await screen.findByRole('heading', { name: 'Welcome' })

    const [, options] = fetchSpy.mock.calls.find(([path]) => path === '/api/auth/register')
    expect(JSON.parse(options.body)).toEqual({
      fullName: 'Ada Lovelace',
      email: 'ada@example.com',
      password: 'long-enough',
    })
  })

  it('shows validation messages next to their fields', async () => {
    mockApi({
      'POST /api/auth/register': [
        400,
        { status: 400, message: 'Validation failed', fieldErrors: { email: 'must be a well-formed email address' } },
      ],
    })
    renderApp('/register')

    await fillIn(userEvent.setup())

    expect(await screen.findByText('must be a well-formed email address')).toBeInTheDocument()
    expect(screen.getByLabelText('Email')).toHaveAttribute('aria-invalid', 'true')
  })

  it('tells the user when the email is taken', async () => {
    mockApi({
      'POST /api/auth/register': [409, { status: 409, message: 'This email is already registered' }],
    })
    renderApp('/register')

    await fillIn(userEvent.setup())

    expect(await screen.findByRole('alert')).toHaveTextContent('This email is already registered')
  })
})
