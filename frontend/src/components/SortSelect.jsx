import { useId } from 'react'
import { SORT_OPTIONS } from '../utils/productSort'

/** The "Sort by" menu above product lists (SCRUM-20). The page keeps the chosen value in the URL. */
export default function SortSelect({ value, onChange }) {
  const id = useId()
  return (
    <div className="sort-select">
      <label htmlFor={id}>Sort by</label>
      <select id={id} value={value} onChange={(event) => onChange(event.target.value)}>
        {SORT_OPTIONS.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
    </div>
  )
}
