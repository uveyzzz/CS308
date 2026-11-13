import { Link } from 'react-router-dom'
import { formatPrice } from '../utils/money'

const LOW_STOCK = 5

function stockBadge(stock) {
  if (stock <= 0) return { label: 'Out of stock', tone: 'out' }
  if (stock <= LOW_STOCK) return { label: `Only ${stock} left`, tone: 'low' }
  return { label: 'In stock', tone: 'in' }
}

export default function ProductCard({ product }) {
  const { slug, name, price, discountedPrice, rating, stock, imageUrl } = product
  const hasDiscount = discountedPrice != null && discountedPrice < price
  const badge = stockBadge(stock)

  return (
    <Link to={`/product/${slug}`} className="product-card" aria-label={name}>
      <div className="product-card-image">
        <img src={imageUrl} alt="" loading="lazy" />
        <span className={`stock-badge stock-badge-${badge.tone}`}>{badge.label}</span>
      </div>
      <h3 className="product-card-name">{name}</h3>
      <div className="product-card-row">
        <span className="product-price">
          {hasDiscount ? (
            <>
              <strong>{formatPrice(discountedPrice)}</strong>
              <s aria-label="Original price">{formatPrice(price)}</s>
            </>
          ) : (
            <strong>{formatPrice(price)}</strong>
          )}
        </span>
        <span className="product-rating" aria-label={`Rating ${Number(rating).toFixed(1)} out of 5`}>
          ★ {Number(rating).toFixed(1)}
        </span>
      </div>
    </Link>
  )
}
