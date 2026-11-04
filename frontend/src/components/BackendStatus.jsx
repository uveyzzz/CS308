import { useEffect, useState } from 'react'
import { apiFetch } from '../api/client'

// Small indicator showing whether the frontend can reach the backend.
export default function BackendStatus() {
  const [status, setStatus] = useState('checking')

  useEffect(() => {
    let cancelled = false
    apiFetch('/api/health')
      .then((data) => !cancelled && setStatus(data?.status === 'UP' ? 'up' : 'down'))
      .catch(() => !cancelled && setStatus('down'))
    return () => {
      cancelled = true
    }
  }, [])

  const label = {
    checking: 'Checking backend…',
    up: 'Backend is up',
    down: 'Backend is unreachable',
  }[status]

  return (
    <p className={`backend-status backend-status--${status}`} role="status">
      {label}
    </p>
  )
}
