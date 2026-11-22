import { apiFetch } from './client'

export const PAGE_SIZE = 12

/**
 * Product list. Reads only the products table on the backend (no product_details).
 * The UI counts pages from 1, Spring counts from 0.
 * With { category: 'women' } it lists that category and its subcategories.
 * With { sort } it orders by 'newest' (the default), 'price_asc', 'price_desc' or 'popular' (SCRUM-20).
 * Item: { id, slug, name, price, discountedPrice, rating, stock, imageUrl }
 * Response: { content: [...], totalPages }
 */
export function fetchProducts(page = 1, size = PAGE_SIZE, { category, sort } = {}) {
  const categoryParam = category ? `&category=${encodeURIComponent(category)}` : ''
  const sortParam = sort ? `&sort=${encodeURIComponent(sort)}` : ''
  return apiFetch(`/api/products?page=${page - 1}&size=${size}${categoryParam}${sortParam}`)
}

/**
 * One product with its details and variants.
 * { id, slug, name, price, discountedPrice, images: [url], description, model, serialNumber,
 *   warrantyStatus, distributor, variants: [{ id, size, color, stock }] }
 */
export function fetchProduct(slug) {
  return apiFetch(`/api/products/${encodeURIComponent(slug)}`)
}
