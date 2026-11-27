import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { fetchProducts, PAGE_SIZE } from '../api/products'
import ProductCard from '../components/ProductCard'
import ProductCardSkeleton from '../components/ProductCardSkeleton'
import SortSelect from '../components/SortSelect'
import { apiSort, listParams, sortFromParams } from '../utils/productSort'
import './products.css'

export default function ProductListPage() {
  const [params, setParams] = useSearchParams()
  const page = Math.max(1, Number(params.get('page')) || 1)
  const sort = sortFromParams(params)
  const key = `${page}:${sort}`
  const [state, setState] = useState({ key: null, items: [], totalPages: 1, error: null })

  useEffect(() => {
    let cancelled = false
    fetchProducts(page, PAGE_SIZE, { sort: apiSort(sort) })
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
  }, [key, page, sort])

  function goTo(next) {
    setParams(listParams({ page: next, sort }))
  }

  /** A new sort starts again from page 1. */
  function changeSort(next) {
    setParams(listParams({ page: 1, sort: next }))
  }

  // Results belong to one page and sort; until they arrive we show skeletons.
  const loading = state.key !== key
  const { items, totalPages, error } = state

  return (
    <section>
      <div className="list-toolbar">
        <h1>Products</h1>
        <SortSelect value={sort} onChange={changeSort} />
      </div>
      {error && <p role="alert" className="form-error">Could not load products: {error}</p>}
      {!loading && !error && items.length === 0 && <p>No products found.</p>}

      <div className="product-grid" aria-busy={loading}>
        {loading
          ? Array.from({ length: PAGE_SIZE }, (_, i) => <ProductCardSkeleton key={i} />)
          : items.map((product) => <ProductCard key={product.id} product={product} />)}
      </div>

      {!loading && totalPages > 1 && (
        <nav className="pagination" aria-label="Pagination">
          <button type="button" disabled={page <= 1} onClick={() => goTo(page - 1)}>
            Previous
          </button>
          <span>
            Page {page} of {totalPages}
          </span>
          <button type="button" disabled={page >= totalPages} onClick={() => goTo(page + 1)}>
            Next
          </button>
        </nav>
      )}
    </section>
  )
}
