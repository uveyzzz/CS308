import { render } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { vi } from 'vitest'
import App from '../App'

export const USER = {
  id: 1,
  fullName: 'Ada Lovelace',
  email: 'ada@example.com',
  role: 'CUSTOMER',
  homeAddress: 'Istanbul',
}

/**
 * Replaces the backend. Routes look like { 'POST /api/auth/login': [200, { token: 'abc' }] }.
 * A request with no route fails the test, so nothing is called by accident.
 */
export function mockApi(routes = {}) {
  const allRoutes = { 'GET /api/health': [200, { status: 'UP' }], ...routes }
  return vi.spyOn(globalThis, 'fetch').mockImplementation(async (path, { method }) => {
    const route = allRoutes[`${method} ${path}`]
    if (!route) throw new Error(`Unexpected request: ${method} ${path}`)
    const [status, body] = route
    return new Response(body === undefined ? null : JSON.stringify(body), { status })
  })
}

/** Renders the whole app at a path, as if the user opened that address. */
export function renderApp(path = '/') {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <App />
    </MemoryRouter>,
  )
}
