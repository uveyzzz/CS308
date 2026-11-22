import { useEffect, useState } from 'react'
import { NavLink } from 'react-router-dom'
import { fetchCategoryTree } from '../api/categories'
import './categories.css'

/**
 * Category menu in the header. Each top-level category opens a list of its subcategories
 * on hover or keyboard focus. If the categories cannot be loaded, the menu is left out and
 * the rest of the header still works.
 */
export default function CategoryMenu() {
  const [tree, setTree] = useState([])

  useEffect(() => {
    let cancelled = false
    fetchCategoryTree()
      .then((categories) => {
        if (!cancelled) setTree(Array.isArray(categories) ? categories : [])
      })
      .catch(() => {
        if (!cancelled) setTree([])
      })
    return () => {
      cancelled = true
    }
  }, [])

  if (tree.length === 0) return null

  return (
    <nav className="category-menu" aria-label="Categories">
      <ul>
        {tree.map((category) => (
          <li key={category.id} className="category-menu-item">
            <NavLink to={`/category/${category.slug}`}>{category.name}</NavLink>
            {category.children.length > 0 && (
              <ul className="category-submenu" aria-label={`${category.name} subcategories`}>
                {category.children.map((child) => (
                  <li key={child.id}>
                    <NavLink to={`/category/${child.slug}`}>{child.name}</NavLink>
                  </li>
                ))}
              </ul>
            )}
          </li>
        ))}
      </ul>
    </nav>
  )
}
