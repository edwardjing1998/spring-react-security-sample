import React, { useEffect, useState } from 'react'
import { createRoot } from 'react-dom/client'
import { api, tokenStore } from './api'
import './styles.css'

function App() {
  const [mode, setMode] = useState('login')
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(Boolean(tokenStore.get()))

  useEffect(() => {
    if (!tokenStore.get()) return
    api('/api/users/me')
      .then(setUser)
      .catch(() => tokenStore.clear())
      .finally(() => setLoading(false))
  }, [])

  function authenticated(result) {
    tokenStore.set(result.accessToken)
    setUser(result.user)
  }

  function logout() {
    tokenStore.clear()
    setUser(null)
    setMode('login')
  }

  return (
    <main className="shell">
      <section className="brand-panel">
        <div className="brand"><span className="brand-mark">S</span> SecureDesk</div>
        <div className="brand-copy">
          <p className="eyebrow">SPRING SECURITY + REACT</p>
          <h1>Identity that feels simple. Security that is not.</h1>
          <p className="lead">A working reference for JWT authentication, protected APIs, and role-based access.</p>
          <ul className="features">
            <li><span>✓</span> BCrypt password protection</li>
            <li><span>✓</span> Short-lived signed access tokens</li>
            <li><span>✓</span> USER and ADMIN authorization</li>
          </ul>
        </div>
        <p className="fine-print">Educational starter · Adapt controls for production</p>
      </section>

      <section className="content-panel">
        {loading ? <div className="spinner" aria-label="Loading" /> : user ? (
          <Dashboard user={user} onLogout={logout} />
        ) : (
          <AuthCard mode={mode} setMode={setMode} onAuthenticated={authenticated} />
        )}
      </section>
    </main>
  )
}

function AuthCard({ mode, setMode, onAuthenticated }) {
  const signup = mode === 'signup'
  const [form, setForm] = useState({ name: '', email: '', password: '' })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function submit(event) {
    event.preventDefault()
    setBusy(true)
    setError('')
    try {
      const result = await api(`/api/auth/${signup ? 'signup' : 'login'}`, {
        method: 'POST',
        body: JSON.stringify(signup ? form : { email: form.email, password: form.password }),
      })
      onAuthenticated(result)
    } catch (e) {
      setError(e.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="card">
      <div className="tabs" role="tablist">
        <button className={!signup ? 'active' : ''} onClick={() => setMode('login')}>Log in</button>
        <button className={signup ? 'active' : ''} onClick={() => setMode('signup')}>Sign up</button>
      </div>
      <div className="card-heading">
        <p className="eyebrow">{signup ? 'CREATE YOUR ACCOUNT' : 'WELCOME BACK'}</p>
        <h2>{signup ? 'Start securely' : 'Log in to your workspace'}</h2>
        <p>{signup ? 'Use a strong password to create your profile.' : 'Enter your credentials to continue.'}</p>
      </div>

      <form onSubmit={submit}>
        {signup && <Field label="Full name" type="text" value={form.name}
          onChange={(name) => setForm({ ...form, name })} autoComplete="name" />}
        <Field label="Email address" type="email" value={form.email}
          onChange={(email) => setForm({ ...form, email })} autoComplete="email" />
        <Field label="Password" type="password" value={form.password}
          onChange={(password) => setForm({ ...form, password })}
          autoComplete={signup ? 'new-password' : 'current-password'} />
        {signup && <p className="password-hint">12+ characters with uppercase, lowercase, and a number.</p>}
        {error && <div className="error" role="alert">{error}</div>}
        <button className="primary" disabled={busy}>{busy ? 'Please wait…' : signup ? 'Create account' : 'Log in'}</button>
      </form>
      {!signup && <p className="demo-note">Demo admin: <b>admin@example.com</b> / <b>ChangeMe123!</b></p>}
    </div>
  )
}

function Field({ label, onChange, ...props }) {
  return <label className="field"><span>{label}</span><input required onChange={(e) => onChange(e.target.value)} {...props} /></label>
}

function Dashboard({ user, onLogout }) {
  const [adminResult, setAdminResult] = useState(null)
  const [error, setError] = useState('')

  async function checkAdmin() {
    setError('')
    try { setAdminResult(await api('/api/admin/summary')) }
    catch (e) { setError(e.status === 403 ? 'Access denied: this endpoint requires the ADMIN role.' : e.message) }
  }

  return (
    <div className="dashboard">
      <div className="dashboard-top"><div className="avatar">{user.name.charAt(0).toUpperCase()}</div><button className="ghost" onClick={onLogout}>Log out</button></div>
      <p className="eyebrow">AUTHENTICATED SESSION</p>
      <h2>Hello, {user.name}</h2>
      <p>Your JWT was accepted and this protected profile was loaded from the API.</p>
      <div className="profile-grid">
        <div><span>Email</span><strong>{user.email}</strong></div>
        <div><span>Role</span><strong className="role">{user.role}</strong></div>
        <div><span>User ID</span><strong>#{user.id}</strong></div>
        <div><span>Created</span><strong>{new Date(user.createdAt).toLocaleDateString()}</strong></div>
      </div>
      <div className="admin-box">
        <h3>Authorization check</h3>
        <p>Call an endpoint restricted to users with the ADMIN role.</p>
        <button className="secondary" onClick={checkAdmin}>Call admin API</button>
        {adminResult && <div className="success">{adminResult.message} Registered users: {adminResult.registeredUsers}.</div>}
        {error && <div className="error">{error}</div>}
      </div>
    </div>
  )
}

createRoot(document.getElementById('root')).render(<React.StrictMode><App /></React.StrictMode>)

