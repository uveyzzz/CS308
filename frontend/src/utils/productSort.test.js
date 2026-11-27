import { apiSort, DEFAULT_SORT, listParams, sortFromParams } from './productSort'

describe('productSort', () => {
  it('reads a known sort from the URL', () => {
    expect(sortFromParams(new URLSearchParams('sort=price_asc'))).toBe('price_asc')
  })

  it('falls back to newest for a missing or unknown sort', () => {
    expect(sortFromParams(new URLSearchParams(''))).toBe(DEFAULT_SORT)
    expect(sortFromParams(new URLSearchParams('sort=cheapest'))).toBe(DEFAULT_SORT)
  })

  it('sends no sort to the API for the default', () => {
    expect(apiSort('newest')).toBeUndefined()
    expect(apiSort('popular')).toBe('popular')
  })

  it('leaves page 1 and the default sort out of the URL', () => {
    expect(listParams({ page: 1, sort: 'newest' })).toEqual({})
    expect(listParams({ page: 3, sort: 'price_desc', q: 'gömlek' })).toEqual({ q: 'gömlek', page: '3', sort: 'price_desc' })
  })
})
