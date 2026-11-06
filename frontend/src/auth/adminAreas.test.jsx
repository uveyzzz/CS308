import { screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { setToken } from '../api/client'
import { USER, mockApi, renderApp } from '../test/renderApp'
import { ADMIN_AREAS } from './adminAreas'

function logInAs(role) {
  setToken('abc')
  mockApi({ 'GET /api/me': [200, { ...USER, role }] })
}

describe('admin areas', () => {
  it.each(ADMIN_AREAS)('shows $role only the link to their own area', async ({ role, label }) => {
    logInAs(role)
    renderApp('/')

    await screen.findByRole('link', { name: label })
    const otherAreas = ADMIN_AREAS.filter((area) => area.role !== role)
    otherAreas.forEach((area) => {
      expect(screen.queryByRole('link', { name: area.label })).not.toBeInTheDocument()
    })
  })

  it('shows a customer no admin links', async () => {
    logInAs('CUSTOMER')
    renderApp('/')

    await screen.findByRole('button', { name: 'Log out' })
    ADMIN_AREAS.forEach(({ label }) => {
      expect(screen.queryByRole('link', { name: label })).not.toBeInTheDocument()
    })
  })

  it('shows a guest no admin links', async () => {
    mockApi()
    renderApp('/')

    await screen.findByRole('link', { name: 'Log in' })
    ADMIN_AREAS.forEach(({ label }) => {
      expect(screen.queryByRole('link', { name: label })).not.toBeInTheDocument()
    })
  })

  it('opens an area for its own role', async () => {
    logInAs('SALES_MANAGER')
    renderApp('/admin/sales')

    expect(await screen.findByRole('heading', { name: 'Sales management' })).toBeInTheDocument()
  })

  it('sends another role to the home page', async () => {
    logInAs('CUSTOMER')
    renderApp('/admin/sales')

    expect(await screen.findByRole('heading', { name: 'Welcome' })).toBeInTheDocument()
  })

  it('sends a guest to the login page', async () => {
    mockApi()
    renderApp('/admin/products')

    expect(await screen.findByRole('heading', { name: 'Log in' })).toBeInTheDocument()
  })
})
