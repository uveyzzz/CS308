import { useState } from 'react'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/authContext'
import FormField from '../components/FormField'

// Requirement 14: the customer sees their name, email address and home address.
export default function ProfilePage() {
  const { user, saveProfile } = useAuth()
  const [fullName, setFullName] = useState(user.fullName)
  const [homeAddress, setHomeAddress] = useState(user.homeAddress ?? '')
  const [status, setStatus] = useState(null)
  const [error, setError] = useState(null)
  const [fieldErrors, setFieldErrors] = useState({})

  async function handleSubmit(event) {
    event.preventDefault()
    setStatus(null)
    setError(null)
    setFieldErrors({})
    try {
      await saveProfile({ fullName, homeAddress })
      setStatus('Your profile was saved.')
    } catch (err) {
      setError(err.message)
      if (err instanceof ApiError) setFieldErrors(err.fieldErrors)
    }
  }

  return (
    <section className="form-page">
      <h1>My profile</h1>
      <p>
        <strong>Email:</strong> {user.email}
      </p>
      <form onSubmit={handleSubmit}>
        <FormField
          label="Full name"
          autoComplete="name"
          required
          value={fullName}
          error={fieldErrors.fullName}
          onChange={(event) => setFullName(event.target.value)}
        />
        <FormField
          label="Home address"
          autoComplete="street-address"
          value={homeAddress}
          error={fieldErrors.homeAddress}
          onChange={(event) => setHomeAddress(event.target.value)}
        />
        {error && (
          <p className="form-error" role="alert">
            {error}
          </p>
        )}
        {status && (
          <p className="form-success" role="status">
            {status}
          </p>
        )}
        <button type="submit">Save</button>
      </form>
    </section>
  )
}
