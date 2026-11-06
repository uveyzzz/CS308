import { useState } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/authContext'
import FormField from '../components/FormField'

const EMPTY_FORM = { fullName: '', email: '', password: '', homeAddress: '' }

export default function RegisterPage() {
  const { user, register } = useAuth()
  const [form, setForm] = useState(EMPTY_FORM)
  const [error, setError] = useState(null)
  const [fieldErrors, setFieldErrors] = useState({})
  const [submitting, setSubmitting] = useState(false)

  if (user) return <Navigate to="/" replace />

  const fieldProps = (name) => ({
    value: form[name],
    error: fieldErrors[name],
    onChange: (event) => setForm({ ...form, [name]: event.target.value }),
  })

  async function handleSubmit(event) {
    event.preventDefault()
    setError(null)
    setFieldErrors({})
    setSubmitting(true)
    try {
      await register({ ...form, homeAddress: form.homeAddress.trim() || undefined })
    } catch (err) {
      setError(err.message)
      if (err instanceof ApiError) setFieldErrors(err.fieldErrors)
      setSubmitting(false)
    }
  }

  return (
    <section className="form-page">
      <h1>Create an account</h1>
      <form onSubmit={handleSubmit}>
        <FormField label="Full name" autoComplete="name" required {...fieldProps('fullName')} />
        <FormField label="Email" type="email" autoComplete="email" required {...fieldProps('email')} />
        <FormField
          label="Password"
          type="password"
          autoComplete="new-password"
          required
          minLength={8}
          {...fieldProps('password')}
          error={fieldErrors.password ?? fieldErrors.passwordShortEnough}
        />
        <FormField label="Home address (optional)" autoComplete="street-address" {...fieldProps('homeAddress')} />
        {error && (
          <p className="form-error" role="alert">
            {error}
          </p>
        )}
        <button type="submit" disabled={submitting}>
          Create account
        </button>
      </form>
      <p>
        Already have an account? <Link to="/login">Log in</Link>
      </p>
    </section>
  )
}
