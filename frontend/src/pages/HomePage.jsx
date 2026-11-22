import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { fetchCategoryTree } from '../api/categories'
import { fetchProducts } from '../api/products'
import BackendStatus from '../components/BackendStatus'
import ProductCard from '../components/ProductCard'
import ProductCardSkeleton from '../components/ProductCardSkeleton'
import './home.css'
import './products.css'

const ROW_SIZE = 4

/** Loads one row of products; null while loading, [] if it fails. */
function useProductRow(sort) {
  const [products, setProducts] = useState(null)

  useEffect(() => {
    let cancelled = false
    fetchProducts(1, ROW_SIZE, { sort })
      .then((page) => {
        if (!cancelled) setProducts(Array.isArray(page?.content) ? page.content : [])
      })
      .catch(() => {
        if (!cancelled) setProducts([])
      })
    return () => {
      cancelled = true
    }
  }, [sort])

  return products
}

/**
 * Home page (SCRUM-28): hero, featured categories, new arrivals and best sellers.
 * Best sellers use the popularity sort from SCRUM-20: most sold first, rating breaks ties.
 * If a section cannot load or has nothing to show, it is left out and the rest still works.
 */
export default function HomePage() {
  const [categories, setCategories] = useState([])
  const newArrivals = useProductRow('newest')
  const bestSellers = useProductRow('popular')

  useEffect(() => {
    let cancelled = false
    fetchCategoryTree()
      .then((tree) => {
        if (!cancelled) setCategories(Array.isArray(tree) ? tree : [])
      })
      .catch(() => {})
    return () => {
      cancelled = true
    }
  }, [])

  return (
    <div className="home">
      <section className="home-hero">
        <h1>Welcome</h1>
        <p>Everyday clothing for women, men and kids.</p>
        <Link to="/products" className="primary-button home-hero-button">
          Shop all products
        </Link>
      </section>

      {categories.length > 0 && (
        <section className="home-section" aria-labelledby="home-categories">
          <h2 id="home-categories">Shop by category</h2>
          <ul className="home-categories">
            {categories.map((category) => (
              <li key={category.id}>
                <Link to={`/category/${category.slug}`} className="home-category">
                  {category.name}
                </Link>
              </li>
            ))}
          </ul>
        </section>
      )}

      <ProductRow id="home-new" title="New arrivals" products={newArrivals} seeAll="/products" />
      <ProductRow id="home-best" title="Best sellers" products={bestSellers} seeAll="/products?sort=popular" />

      <BackendStatus />
    </div>
  )
}

function ProductRow({ id, title, products, seeAll }) {
  if (products?.length === 0) return null

  return (
    <section className="home-section" aria-labelledby={id}>
      <div className="home-section-title">
        <h2 id={id}>{title}</h2>
        <Link to={seeAll}>See all</Link>
      </div>
      <div className="product-grid">
        {products === null
          ? Array.from({ length: ROW_SIZE }, (_, i) => <ProductCardSkeleton key={i} />)
          : products.map((product) => <ProductCard key={product.id} product={product} />)}
      </div>
    </section>
  )
}
