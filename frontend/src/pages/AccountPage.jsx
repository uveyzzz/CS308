import { useOutletContext, useNavigate } from 'react-router-dom'

export default function AccountPage() {
  const { user, setUser } = useOutletContext()
  const navigate = useNavigate()

  if (!user) {
    return (
      <div className="account-container">
        <h2>Lütfen önce giriş yapın.</h2>
      </div>
    )
  }

  const handleLogout = () => {
    setUser(null)
    navigate('/')
  }

  return (
    <div className="account-container">
      <h1>Hesabım</h1>
      <h2>Merhaba, {user.name}</h2>
      
      <div className="account-dashboard">
        <div className="account-menu">
          <button className="active">Siparişlerim</button>
          <button>Kullanıcı Bilgilerim</button>
          <button>Adreslerim</button>
          <button onClick={handleLogout}>Çıkış Yap</button>
        </div>
        <div className="account-content">
          <h3>Sipariş Geçmişi</h3>
          <p>Sipariş #98241 - Hazırlanıyor</p>
        </div>
      </div>
    </div>
  )
}