import { Route, Routes } from 'react-router-dom'
import AuthProvider from './auth/AuthProvider'
import ProtectedRoute from './auth/ProtectedRoute'
import { ADMIN_AREAS } from './auth/adminAreas'
import Layout from './components/Layout'
import AdminPage from './pages/AdminPage'
import HomePage from './pages/HomePage'
import LoginPage from './pages/LoginPage'
import NotFoundPage from './pages/NotFoundPage'
import ProfilePage from './pages/ProfilePage'
import RegisterPage from './pages/RegisterPage'

// Routes live here. Add a page as <Route path="products" element={<ProductListPage />} />
// Pages that need a login go inside the ProtectedRoute block.
export default function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route element={<Layout />}>
          <Route index element={<HomePage />} />
          <Route path="login" element={<LoginPage />} />
          <Route path="register" element={<RegisterPage />} />
          <Route element={<ProtectedRoute />}>
            <Route path="account/profile" element={<ProfilePage />} />
          </Route>
          {ADMIN_AREAS.map(({ role, path, label }) => (
            <Route key={path} element={<ProtectedRoute roles={[role]} />}>
              <Route path={path} element={<AdminPage title={label} />} />
            </Route>
          ))}
          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Routes>
    </AuthProvider>
  )
}
