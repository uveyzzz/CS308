import { useState } from 'react'
import { createSearchParams, useNavigate } from 'react-router-dom'

/** The search box in the header (SCRUM-19). Submitting it opens /search?q=... */
export default function SearchBar({ onSearch }) {
  const navigate = useNavigate()
  const [text, setText] = useState('')

  function handleSubmit(event) {
    // A form would normally reload the whole page; the app changes the URL itself instead.
    event.preventDefault()
    const q = text.trim()
    if (!q) return
    navigate(`/search?${createSearchParams({ q })}`)
    onSearch?.()
  }

  return (
    <form className="search-bar" role="search" onSubmit={handleSubmit}>
      <input
        type="search"
        aria-label="Search products"
        placeholder="Search products"
        value={text}
        onChange={(event) => setText(event.target.value)}
      />
      <button type="submit">Search</button>
    </form>
  )
}
