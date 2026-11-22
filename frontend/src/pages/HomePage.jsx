import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { fetchCategoryTree } from '../api/categories'
import { fetchProducts } from '../api/products'
import BackendStatus from '../components/BackendStatus'
import ProductCard from '../components/ProductCard'
import ProductCardSkeleton from '../components/ProductCardSkeleton'
import './home.css'
import './products.css'

const NEW_ARRIVALS = 4

/**
 * Home page (SCRUM-28): hero, featured categories and new arrivals.
 * The product list is already sorted newest first. A best sellers section needs the
 * sorting from SCRUM-20 and is added when that lands.
 * If a section cannot load, it is left out and the rest of the page still works.
 */
export default function HomePage() {
  const [categories, setCategories] = useState([])
  const [newArrivals, setNewArrivals] = useState(null) // null while loading

  useEffect(() => {
    let cancelled = false
    fetchCategoryTree()
      .then((tree) => {
        if (!cancelled) setCategories(Array.isArray(tree) ? tree : [])
      })
      .catch(() => {})
    fetchProducts(1, NEW_ARRIVALS)
      .then((page) => {
        if (!cancelled) setNewArrivals(Array.isArray(page?.content) ? page.content : [])
      })
      .catch(() => {
        if (!cancelled) setNewArrivals([])
      })
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

      {newArrivals?.length !== 0 && (
        <section className="home-section" aria-labelledby="home-new">
          <div className="home-section-title">
            <h2 id="home-new">New arrivals</h2>
            <Link to="/products">See all</Link>
          </div>
          <div className="product-grid">
            {newArrivals === null
              ? Array.from({ length: NEW_ARRIVALS }, (_, i) => <ProductCardSkeleton key={i} />)
              : newArrivals.map((product) => <ProductCard key={product.id} product={product} />)}
          </div>
        </section>
      )}

      <BackendStatus />
    </div>
  )
}
