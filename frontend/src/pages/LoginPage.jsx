import { useState } from 'react'
import { useNavigate, useOutletContext } from 'react-router-dom'

export default function LoginPage() {
  const [email, setEmail] = useState('')
  const { setUser } = useOutletContext()
  const navigate = useNavigate()

  const handleLogin = (e) => {
    e.preventDefault()
    // Örnek başarılı giriş simülasyonu
    setUser({ name: 'Elif', email })
    navigate('/account')
  }

  return (
    <div className="login-container">
      <div className="login-box">
        <h2>Giriş Yap</h2>
        <p>Hesabınıza erişin veya yeni bir hesap oluşturun</p>
        <form onSubmit={handleLogin}>
          <input 
            type="email" 
            placeholder="E-posta" 
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
          <input type="password" placeholder="Şifre" required />
          <button type="submit" className="submit-btn">GİRİŞ YAP</button>
        </form>
      </div>
    </div>
  )
}