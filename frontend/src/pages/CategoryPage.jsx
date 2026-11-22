import { useEffect, useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import { fetchCategory } from '../api/categories'
import { fetchProducts, PAGE_SIZE } from '../api/products'
import ProductCard from '../components/ProductCard'
import ProductCardSkeleton from '../components/ProductCardSkeleton'
import '../components/categories.css'
import './products.css'

/**
 * /category/:slug lists the products of a category and its subcategories, with a breadcrumb
 * and links to the subcategories (SCRUM-16).
 */
export default function CategoryPage() {
  const { slug } = useParams()
  const [params, setParams] = useSearchParams()
  const page = Math.max(1, Number(params.get('page')) || 1)
  const key = `${slug}:${page}`
  const [state, setState] = useState({ key: null, category: null, items: [], totalPages: 1, error: null })

  useEffect(() => {
    let cancelled = false
    Promise.all([fetchCategory(slug), fetchProducts(page, PAGE_SIZE, { category: slug })])
      .then(([category, products]) => {
        if (cancelled) return
        setState({ key, category, items: products.content ?? [], totalPages: products.totalPages ?? 1, error: null })
      })
      .catch((error) => {
        if (cancelled) return
        setState({ key, category: null, items: [], totalPages: 1, error })
      })
    return () => {
      cancelled = true
    }
  }, [key, slug, page])

  // Results belong to one category and page; until they arrive we show skeletons.
  const loading = state.key !== key
  const { category, items, totalPages, error } = state

  if (!loading && error?.status === 404) {
    return (
      <section>
        <h1>Category not found</h1>
        <p>
          <Link to="/products">See all products</Link>
        </p>
      </section>
    )
  }

  return (
    <section>
      {category && (
        <nav className="breadcrumb" aria-label="Breadcrumb">
          <ol>
            <li>
              <Link to="/">Home</Link>
            </li>
            {category.path.map((crumb, i) => (
              <li key={crumb.slug}>
                {i === category.path.length - 1 ? (
                  <span aria-current="page">{crumb.name}</span>
                ) : (
                  <Link to={`/category/${crumb.slug}`}>{crumb.name}</Link>
                )}
              </li>
            ))}
          </ol>
        </nav>
      )}

      <h1>{category?.name ?? 'Products'}</h1>
      {error && <p role="alert" className="form-error">Could not load this category: {error.message}</p>}

      {category?.children.length > 0 && (
        <ul className="subcategory-links" aria-label="Subcategories">
          {category.children.map((child) => (
            <li key={child.id}>
              <Link to={`/category/${child.slug}`}>{child.name}</Link>
            </li>
          ))}
        </ul>
      )}

      {!loading && !error && items.length === 0 && (
        <p>
          No products in this category yet. <Link to="/products">See all products</Link>
        </p>
      )}

      <div className="product-grid" aria-busy={loading}>
        {loading
          ? Array.from({ length: PAGE_SIZE }, (_, i) => <ProductCardSkeleton key={i} />)
          : items.map((product) => <ProductCard key={product.id} product={product} />)}
      </div>

      {!loading && totalPages > 1 && (
        <nav className="pagination" aria-label="Pagination">
          <button type="button" disabled={page <= 1} onClick={() => setParams({ page: String(page - 1) })}>
            Previous
          </button>
          <span>
            Page {page} of {totalPages}
          </span>
          <button type="button" disabled={page >= totalPages} onClick={() => setParams({ page: String(page + 1) })}>
            Next
          </button>
        </nav>
      )}
    </section>
  )
}
