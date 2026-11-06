// Single place for talking to the backend. Every page should call apiFetch
// instead of fetch directly, so auth headers and error handling stay consistent.

const TOKEN_KEY = 'token'

/** Fired when the backend rejects the stored token, so the app can show the guest view. */
export const SESSION_EXPIRED_EVENT = 'session-expired'

export class ApiError extends Error {
  constructor(status, message, fieldErrors = {}) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token) {
  if (token) localStorage.setItem(TOKEN_KEY, token)
  else localStorage.removeItem(TOKEN_KEY)
}

/**
 * apiFetch('/api/products')                       -> GET, parsed JSON
 * apiFetch('/api/cart', { method: 'POST', body: { productId: 1 } })
 * Throws ApiError (status, message, fieldErrors) when the response is not 2xx.
 */
export async function apiFetch(path, { method = 'GET', body, headers = {} } = {}) {
  const finalHeaders = { Accept: 'application/json', ...headers }
  if (body !== undefined) finalHeaders['Content-Type'] = 'application/json'

  const token = getToken()
  if (token) finalHeaders.Authorization = `Bearer ${token}`

  const response = await fetch(path, {
    method,
    headers: finalHeaders,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })

  if (response.status === 401 && token) {
    setToken(null)
    window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT))
  }

  const text = await response.text()
  const data = text ? JSON.parse(text) : null

  if (!response.ok) {
    // Backend errors follow the ApiError record: { status, message, fieldErrors }
    throw new ApiError(
      response.status,
      data?.message ?? `Request failed (${response.status})`,
      data?.fieldErrors ?? {},
    )
  }
  return data
}
