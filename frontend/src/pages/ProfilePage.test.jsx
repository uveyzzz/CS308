import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it } from 'vitest'
import { setToken } from '../api/client'
import { USER, mockApi, renderApp } from '../test/renderApp'

describe('ProfilePage', () => {
  beforeEach(() => setToken('abc'))

  it('shows the name, email and home address (requirement 14)', async () => {
    mockApi({ 'GET /api/me': [200, USER] })
    renderApp('/account/profile')

    expect(await screen.findByLabelText('Full name')).toHaveValue('Ada Lovelace')
    expect(screen.getByLabelText('Home address')).toHaveValue('Istanbul')
    expect(screen.getByText('ada@example.com')).toBeInTheDocument()
  })

  it('saves changes', async () => {
    const fetchSpy = mockApi({
      'GET /api/me': [200, USER],
      'PATCH /api/me': [200, { ...USER, homeAddress: 'Ankara' }],
    })
    const user = userEvent.setup()
    renderApp('/account/profile')

    const address = await screen.findByLabelText('Home address')
    await user.clear(address)
    await user.type(address, 'Ankara')
    await user.click(screen.getByRole('button', { name: 'Save' }))

    expect(await screen.findByRole('status')).toHaveTextContent('Your profile was saved.')
    const [, options] = fetchSpy.mock.calls.find(([, { method }]) => method === 'PATCH')
    expect(JSON.parse(options.body)).toEqual({ fullName: 'Ada Lovelace', homeAddress: 'Ankara' })
  })

  it('shows why a change was rejected', async () => {
    mockApi({
      'GET /api/me': [200, USER],
      'PATCH /api/me': [
        400,
        { status: 400, message: 'Validation failed', fieldErrors: { fullName: 'must not be blank' } },
      ],
    })
    const user = userEvent.setup()
    renderApp('/account/profile')

    await user.click(await screen.findByRole('button', { name: 'Save' }))

    expect(await screen.findByText('must not be blank')).toBeInTheDocument()
  })

  // The acceptance criterion of SCRUM-27.
  it('still shows the saved changes after logging out and back in', async () => {
    const saved = { ...USER, fullName: 'Ada King', homeAddress: 'Ankara' }
    mockApi({
      'GET /api/me': [200, USER],
      'PATCH /api/me': [200, saved],
      'POST /api/auth/login': [200, { token: 'def', user: saved }],
    })
    const user = userEvent.setup()
    renderApp('/account/profile')

    const name = await screen.findByLabelText('Full name')
    await user.clear(name)
    await user.type(name, 'Ada King')
    await user.clear(screen.getByLabelText('Home address'))
    await user.type(screen.getByLabelText('Home address'), 'Ankara')
    await user.click(screen.getByRole('button', { name: 'Save' }))
    await screen.findByRole('status')

    await user.click(screen.getByRole('button', { name: 'Log out' }))
    await user.type(await screen.findByLabelText('Email'), 'ada@example.com')
    await user.type(screen.getByLabelText('Password'), 'long-enough')
    await user.click(screen.getByRole('button', { name: 'Log in' }))

    expect(await screen.findByLabelText('Full name')).toHaveValue('Ada King')
    expect(screen.getByLabelText('Home address')).toHaveValue('Ankara')
  })
})
