import { apiFetch } from './client'

/** The menu: [{ id, name, slug, children: [...] }], top-level categories first. */
export function fetchCategoryTree() {
  return apiFetch('/api/categories')
}

/**
 * One category for its page.
 * { id, name, slug, description, path: [{ name, slug }], children: [{ id, name, slug, children }] }
 * path is the breadcrumb from the top-level category down to this one.
 */
export function fetchCategory(slug) {
  return apiFetch(`/api/categories/${encodeURIComponent(slug)}`)
}
