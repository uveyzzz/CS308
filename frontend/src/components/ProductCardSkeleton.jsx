export default function ProductCardSkeleton() {
  return (
    <div className="product-card" data-testid="product-skeleton" aria-hidden="true">
      <div className="product-card-image skeleton" />
      <div className="skeleton skeleton-line" style={{ width: '70%' }} />
      <div className="skeleton skeleton-line" style={{ width: '40%' }} />
    </div>
  )
}
