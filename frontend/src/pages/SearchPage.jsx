import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { fetchProducts, PAGE_SIZE } from '../api/products'
import ProductCard from '../components/ProductCard'
import ProductCardSkeleton from '../components/ProductCardSkeleton'
import SortSelect from '../components/SortSelect'
import { apiSort, listParams, sortFromParams } from '../utils/productSort'
import './products.css'

/**
 * /search?q=gömlek lists the products whose name or description contains the text (SCRUM-19).
 * Out-of-stock products are listed too, with their badge (requirement 7). The search, the sort
 * and the page all live in the URL, so a refresh or a shared link shows the same results.
 */
export default function SearchPage() {
  const [params, setParams] = useSearchParams()
  const q = (params.get('q') ?? '').trim()
  const page = Math.max(1, Number(params.get('page')) || 1)
  const sort = sortFromParams(params)
  const key = `${q}:${page}:${sort}`
  const [state, setState] = useState({ key: null, items: [], totalPages: 1, error: null })

  useEffect(() => {
    if (!q) return
    let cancelled = false
    fetchProducts(page, PAGE_SIZE, { sort: apiSort(sort), q })
      .then((data) => {
        if (cancelled) return
        setState({ key, items: data.content ?? [], totalPages: data.totalPages ?? 1, error: null })
      })
      .catch((error) => {
        if (cancelled) return
        setState({ key, items: [], totalPages: 1, error: error.message })
      })
    return () => {
      cancelled = true
    }
  }, [key, q, page, sort])

  if (!q) {
    return (
      <section>
        <h1>Search</h1>
        <p>Type a product name or a word from its description in the search bar.</p>
      </section>
    )
  }

  // Results belong to one search, page and sort; until they arrive we show skeletons.
  const loading = state.key !== key
  const { items, totalPages, error } = state

  return (
    <section>
      <div className="list-toolbar">
        <h1>Results for "{q}"</h1>
        <SortSelect value={sort} onChange={(next) => setParams(listParams({ q, page: 1, sort: next }))} />
      </div>
      {error && <p role="alert" className="form-error">Could not search: {error}</p>}
      {!loading && !error && items.length === 0 && <p>No products match "{q}".</p>}

      <div className="product-grid" aria-busy={loading}>
        {loading
          ? Array.from({ length: PAGE_SIZE }, (_, i) => <ProductCardSkeleton key={i} />)
          : items.map((product) => <ProductCard key={product.id} product={product} />)}
      </div>

      {!loading && totalPages > 1 && (
        <nav className="pagination" aria-label="Pagination">
          <button type="button" disabled={page <= 1} onClick={() => setParams(listParams({ q, page: page - 1, sort }))}>
            Previous
          </button>
          <span>
            Page {page} of {totalPages}
          </span>
          <button type="button" disabled={page >= totalPages} onClick={() => setParams(listParams({ q, page: page + 1, sort }))}>
            Next
          </button>
        </nav>
      )}
    </section>
  )
}
