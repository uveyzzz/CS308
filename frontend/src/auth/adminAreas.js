// Each admin area belongs to one role. The menu shows a user only their own area, but this
// is only for display: the backend still checks the role on every request.
export const ADMIN_AREAS = [
  { role: 'PRODUCT_MANAGER', path: '/admin/products', label: 'Product management' },
  { role: 'SALES_MANAGER', path: '/admin/sales', label: 'Sales management' },
  { role: 'SUPPORT_AGENT', path: '/admin/support', label: 'Support queue' },
]
