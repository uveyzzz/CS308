import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { fetchProduct } from '../api/products'
import { useCart } from '../cart/cartContext'
import { formatPrice } from '../utils/money'
import './products.css'

const unique = (values) => [...new Set(values)]

export default function ProductDetailPage() {
  const { slug } = useParams()
  const { addItem } = useCart()
  const [state, setState] = useState({ slug: null, status: 'loading', product: null, error: null })
  const [size, setSize] = useState(null)
  const [color, setColor] = useState(null)
  const [quantity, setQuantity] = useState(1)
  const [imageIndex, setImageIndex] = useState(0)
  const [added, setAdded] = useState(null)

  useEffect(() => {
    let cancelled = false
    fetchProduct(slug)
      .then((product) => {
        if (cancelled) return
        // Start on the first variant that can be bought.
        const first = product.variants.find((v) => v.stock > 0) ?? product.variants[0]
        setSize(first?.size ?? null)
        setColor(first?.color ?? null)
        setQuantity(1)
        setImageIndex(0)
        setAdded(null)
        setState({ slug, status: 'ok', product, error: null })
      })
      .catch((error) => {
        if (cancelled) return
        setState({ slug, status: error.status === 404 ? 'notfound' : 'error', product: null, error: error.message })
      })
    return () => {
      cancelled = true
    }
  }, [slug])

  if (state.slug !== slug) return <p aria-busy="true">Loading product…</p>
  if (state.status === 'notfound') {
    return (
      <p>
        Product not found. <Link to="/products">Back to products</Link>
      </p>
    )
  }
  if (state.status === 'error') return <p role="alert" className="form-error">Could not load the product: {state.error}</p>

  const { product } = state
  const sizes = unique(product.variants.map((v) => v.size))
  const colors = unique(product.variants.map((v) => v.color))
  const variant = product.variants.find((v) => v.size === size && v.color === color)
  const stock = variant?.stock ?? 0
  const hasDiscount = product.discountedPrice != null && product.discountedPrice < product.price
  const unitPrice = hasDiscount ? product.discountedPrice : product.price

  // An option is disabled when no variant with that option (and the other selected option) has stock.
  const canBuy = (s, c) => product.variants.some((v) => v.size === s && v.color === c && v.stock > 0)

  function choose(setter, value) {
    setter(value)
    setQuantity(1)
    setAdded(null)
  }

  function handleAdd() {
    if (!variant || stock <= 0) return
    addItem(
      {
        variantId: variant.id,
        productId: product.id,
        slug: product.slug,
        name: product.name,
        size: variant.size,
        color: variant.color,
        price: unitPrice,
        image: product.images?.[0],
      },
      quantity,
      stock,
    )
    setAdded(`${variant.size} / ${variant.color} × ${quantity}`)
  }

  return (
    <article className="product-detail">
      <div className="product-gallery">
        <img className="product-gallery-main" src={product.images[imageIndex]} alt={product.name} />
        <div className="product-gallery-thumbs">
          {product.images.map((src, i) => (
            <button
              key={src + i}
              type="button"
              aria-label={`Show image ${i + 1}`}
              aria-pressed={i === imageIndex}
              onClick={() => setImageIndex(i)}
            >
              <img src={src} alt="" />
            </button>
          ))}
        </div>
      </div>

      <div className="product-info">
        <h1>{product.name}</h1>
        <p className="product-price product-price-large">
          {hasDiscount ? (
            <>
              <strong>{formatPrice(product.discountedPrice)}</strong>
              <s aria-label="Original price">{formatPrice(product.price)}</s>
            </>
          ) : (
            <strong>{formatPrice(product.price)}</strong>
          )}
        </p>
        <p>{product.description}</p>

        <fieldset className="option-group">
          <legend>Size</legend>
          {sizes.map((s) => (
            <button
              key={s}
              type="button"
              className="option-chip"
              aria-pressed={s === size}
              disabled={!canBuy(s, color)}
              onClick={() => choose(setSize, s)}
            >
              {s}
            </button>
          ))}
        </fieldset>

        <fieldset className="option-group">
          <legend>Color</legend>
          {colors.map((c) => (
            <button
              key={c}
              type="button"
              className="option-chip"
              aria-pressed={c === color}
              disabled={!canBuy(size, c)}
              onClick={() => choose(setColor, c)}
            >
              {c}
            </button>
          ))}
        </fieldset>

        <p className={`stock-line ${stock <= 0 ? 'stock-line-out' : ''}`} aria-live="polite">
          {stock <= 0 ? 'Out of stock' : `Stock: ${stock}`}
        </p>

        <div className="buy-row">
          <div className="quantity">
            <button type="button" aria-label="Decrease quantity" disabled={quantity <= 1} onClick={() => setQuantity(quantity - 1)}>
              −
            </button>
            <span aria-label="Quantity">{quantity}</span>
            <button type="button" aria-label="Increase quantity" disabled={quantity >= stock} onClick={() => setQuantity(quantity + 1)}>
              +
            </button>
          </div>
          <button type="button" className="primary-button" disabled={stock <= 0} onClick={handleAdd}>
            Add to cart
          </button>
        </div>
        {added && <p role="status" className="added-message">Added to cart: {added}</p>}

        <dl className="product-specs">
          <dt>Model</dt>
          <dd>{product.model}</dd>
          <dt>Serial number</dt>
          <dd>{product.serialNumber}</dd>
          <dt>Warranty status</dt>
          <dd>{product.warrantyStatus}</dd>
          <dt>Distributor</dt>
          <dd>{product.distributor}</dd>
        </dl>
      </div>
    </article>
  )
}
