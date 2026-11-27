/** Sort options for product lists (SCRUM-20). The values are the ones the backend accepts. */
export const SORT_OPTIONS = [
  { value: 'newest', label: 'Newest' },
  { value: 'price_asc', label: 'Price: low to high' },
  { value: 'price_desc', label: 'Price: high to low' },
  { value: 'popular', label: 'Most popular' },
]

export const DEFAULT_SORT = 'newest'

/** The sort in the URL (?sort=...). A missing or unknown value means the default, like on the backend. */
export function sortFromParams(params) {
  const value = params.get('sort')
  return SORT_OPTIONS.some((option) => option.value === value) ? value : DEFAULT_SORT
}

/** What to send to the API: nothing for the default, so that request stays exactly as before. */
export function apiSort(sort) {
  return sort === DEFAULT_SORT ? undefined : sort
}

/**
 * The URL query of a list page. Page 1 and the default sort are left out to keep links short.
 * Building the whole query here means changing the page never drops the sort (or the search).
 */
export function listParams({ page = 1, sort = DEFAULT_SORT, q } = {}) {
  const params = {}
  if (q) params.q = q
  if (page > 1) params.page = String(page)
  if (sort !== DEFAULT_SORT) params.sort = sort
  return params
}
