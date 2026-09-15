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
      {user.role === 'ADMIN' && <ProfileAdmin />}
      {user.role === 'ADMIN' && <SchoolAdmin />}
      {user.role === 'ADMIN' && <MembershipAdmin />}
    </div>
  )
}

const emptyProfile = {
  userId: '',
  firstName: '',
  lastName: '',
  displayName: '',
  preferredLanguage: 'en-US',
  timezone: 'America/Los_Angeles',
  avatarUrl: '',
}

function ProfileAdmin() {
  const [profiles, setProfiles] = useState([])
  const [users, setUsers] = useState([])
  const [form, setForm] = useState(emptyProfile)
  const [editingUserId, setEditingUserId] = useState(null)
  const [busy, setBusy] = useState(false)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  async function load() {
    setLoading(true)
    setError('')
    try {
      const [profileRows, userRows] = await Promise.all([
        api('/api/admin/user-profiles'),
        api('/api/admin/users'),
      ])
      setProfiles(profileRows)
      setUsers(userRows)
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  function change(name, value) {
    setForm(current => ({ ...current, [name]: value }))
  }

  function resetForm() {
    setEditingUserId(null)
    setForm(emptyProfile)
    setError('')
  }

  function edit(profile) {
    setEditingUserId(profile.userId)
    setForm({
      userId: String(profile.userId),
      firstName: profile.firstName,
      lastName: profile.lastName,
      displayName: profile.displayName || '',
      preferredLanguage: profile.preferredLanguage || 'en-US',
      timezone: profile.timezone || 'America/Los_Angeles',
      avatarUrl: profile.avatarUrl || '',
    })
    setMessage('')
    setError('')
  }

  async function save(event) {
    event.preventDefault()
    setBusy(true)
    setError('')
    setMessage('')
    const details = {
      firstName: form.firstName,
      lastName: form.lastName,
      displayName: form.displayName || null,
      preferredLanguage: form.preferredLanguage || null,
      timezone: form.timezone || null,
      avatarUrl: form.avatarUrl || null,
    }
    try {
      if (editingUserId !== null) {
        await api(`/api/admin/user-profiles/${editingUserId}`, {
          method: 'PUT',
          body: JSON.stringify(details),
        })
        setMessage('User profile updated.')
      } else {
        await api('/api/admin/user-profiles', {
          method: 'POST',
          body: JSON.stringify({ ...details, userId: Number(form.userId) }),
        })
        setMessage('User profile created.')
      }
      resetForm()
      await load()
    } catch (e) {
      setError(e.message)
    } finally {
      setBusy(false)
    }
  }

  async function remove(profile) {
    if (!window.confirm(`Delete the profile for ${profile.userEmail}?`)) return
    setBusy(true)
    setError('')
    setMessage('')
    try {
      await api(`/api/admin/user-profiles/${profile.userId}`, { method: 'DELETE' })
      if (editingUserId === profile.userId) resetForm()
      setMessage('User profile deleted.')
      await load()
    } catch (e) {
      setError(e.message)
    } finally {
      setBusy(false)
    }
  }

  const profiledIds = new Set(profiles.map(profile => profile.userId))
  const availableUsers = users.filter(user => !profiledIds.has(user.id))

  return (
    <section className="profile-admin">
      <div className="section-heading">
        <div>
          <p className="eyebrow">ADMIN ONLY</p>
          <h3>User profile management</h3>
          <p>Create, update, and delete extended user profiles.</p>
        </div>
        {editingUserId !== null && <button className="ghost" onClick={resetForm}>Cancel edit</button>}
      </div>

      <form className="profile-form" onSubmit={save}>
        <label className="field full-width">
          <span>User account</span>
          <select required disabled={editingUserId !== null} value={form.userId}
            onChange={event => change('userId', event.target.value)}>
            <option value="">Select a user</option>
            {editingUserId !== null
              ? users.filter(user => user.id === editingUserId).map(user =>
                  <option key={user.id} value={user.id}>{user.name} — {user.email}</option>)
              : availableUsers.map(user =>
                  <option key={user.id} value={user.id}>{user.name} — {user.email}</option>)}
          </select>
        </label>
        <Field label="First name" type="text" value={form.firstName}
          onChange={value => change('firstName', value)} />
        <Field label="Last name" type="text" value={form.lastName}
          onChange={value => change('lastName', value)} />
        <Field label="Display name" type="text" value={form.displayName}
          required={false} onChange={value => change('displayName', value)} />
        <Field label="Preferred language" type="text" value={form.preferredLanguage}
          required={false} onChange={value => change('preferredLanguage', value)} />
        <Field label="Timezone" type="text" value={form.timezone}
          required={false} onChange={value => change('timezone', value)} />
        <Field label="Avatar URL" type="url" value={form.avatarUrl}
          required={false} onChange={value => change('avatarUrl', value)} />
        <button className="primary full-width" disabled={busy || (!editingUserId && !availableUsers.length)}>
          {busy ? 'Saving…' : editingUserId !== null ? 'Update profile' : 'Create profile'}
        </button>
      </form>

      {message && <div className="success profile-message">{message}</div>}
      {error && <div className="error profile-message">{error}</div>}

      <div className="profile-list">
        <div className="list-heading"><h3>Existing profiles</h3><button className="ghost" onClick={load}>Refresh</button></div>
        {loading ? <div className="spinner small" aria-label="Loading profiles" />
          : profiles.length === 0 ? <p className="empty-state">No user profiles have been created.</p>
          : profiles.map(profile => (
            <article className="profile-row" key={profile.userId}>
              <div className="mini-avatar">{(profile.displayName || profile.firstName).charAt(0).toUpperCase()}</div>
              <div className="profile-summary">
                <strong>{profile.displayName || `${profile.firstName} ${profile.lastName}`}</strong>
                <span>{profile.userEmail}</span>
                <small>{profile.preferredLanguage} · {profile.timezone}</small>
              </div>
              <div className="row-actions">
                <button className="ghost" onClick={() => edit(profile)}>Edit</button>
                <button className="danger" disabled={busy} onClick={() => remove(profile)}>Delete</button>
              </div>
            </article>
          ))}
      </div>
    </section>
  )
}

const emptySchool = {
  schoolCode: '',
  schoolName: '',
  schoolType: '',
  districtName: '',
  addressLine1: '',
  city: '',
  stateCode: '',
  postalCode: '',
  countryCode: 'US',
  status: 'ACTIVE',
}

function SchoolAdmin() {
  const [schools, setSchools] = useState([])
  const [form, setForm] = useState(emptySchool)
  const [editingId, setEditingId] = useState(null)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  async function load() {
    setLoading(true)
    setError('')
    try {
      setSchools(await api('/api/admin/schools'))
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  function change(name, value) {
    setForm(current => ({ ...current, [name]: value }))
  }

  function resetForm() {
    setEditingId(null)
    setForm(emptySchool)
    setError('')
  }

  function edit(school) {
    setEditingId(school.id)
    setForm({
      schoolCode: school.schoolCode,
      schoolName: school.schoolName,
      schoolType: school.schoolType || '',
      districtName: school.districtName || '',
      addressLine1: school.addressLine1 || '',
      city: school.city || '',
      stateCode: school.stateCode || '',
      postalCode: school.postalCode || '',
      countryCode: school.countryCode || 'US',
      status: school.status || 'ACTIVE',
    })
    setError('')
    setMessage('')
  }

  async function save(event) {
    event.preventDefault()
    setBusy(true)
    setError('')
    setMessage('')
    const payload = Object.fromEntries(
      Object.entries(form).map(([key, value]) => [key, value || null])
    )
    try {
      if (editingId !== null) {
        await api(`/api/admin/schools/${editingId}`, {
          method: 'PUT',
          body: JSON.stringify(payload),
        })
        setMessage('School updated.')
      } else {
        await api('/api/admin/schools', {
          method: 'POST',
          body: JSON.stringify(payload),
        })
        setMessage('School created.')
      }
      resetForm()
      await load()
    } catch (e) {
      setError(e.message)
    } finally {
      setBusy(false)
    }
  }

  async function remove(school) {
    if (!window.confirm(`Delete ${school.schoolName}?`)) return
    setBusy(true)
    setError('')
    setMessage('')
    try {
      await api(`/api/admin/schools/${school.id}`, { method: 'DELETE' })
      if (editingId === school.id) resetForm()
      setMessage('School deleted.')
      await load()
    } catch (e) {
      setError(e.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <section className="profile-admin school-admin">
      <div className="section-heading">
        <div>
          <p className="eyebrow">ADMIN ONLY</p>
          <h3>School management</h3>
          <p>Create, update, and delete school records.</p>
        </div>
        {editingId !== null && <button className="ghost" onClick={resetForm}>Cancel edit</button>}
      </div>

      <form className="profile-form school-form" onSubmit={save}>
        <Field label="School code" type="text" value={form.schoolCode}
          onChange={value => change('schoolCode', value)} />
        <Field label="School name" type="text" value={form.schoolName}
          onChange={value => change('schoolName', value)} />
        <Field label="School type" type="text" value={form.schoolType}
          required={false} onChange={value => change('schoolType', value)} />
        <Field label="District" type="text" value={form.districtName}
          required={false} onChange={value => change('districtName', value)} />
        <label className="field full-width">
          <span>Address</span>
          <input type="text" value={form.addressLine1}
            onChange={event => change('addressLine1', event.target.value)} />
        </label>
        <Field label="City" type="text" value={form.city}
          required={false} onChange={value => change('city', value)} />
        <Field label="State code" type="text" value={form.stateCode}
          required={false} onChange={value => change('stateCode', value)} />
        <Field label="Postal code" type="text" value={form.postalCode}
          required={false} onChange={value => change('postalCode', value)} />
        <Field label="Country code" type="text" value={form.countryCode}
          onChange={value => change('countryCode', value)} />
        <label className="field">
          <span>Status</span>
          <select value={form.status} onChange={event => change('status', event.target.value)}>
            <option value="ACTIVE">Active</option>
            <option value="INACTIVE">Inactive</option>
          </select>
        </label>
        <button className="primary full-width" disabled={busy}>
          {busy ? 'Saving…' : editingId !== null ? 'Update school' : 'Create school'}
        </button>
      </form>

      {message && <div className="success profile-message">{message}</div>}
      {error && <div className="error profile-message">{error}</div>}

      <div className="profile-list">
        <div className="list-heading"><h3>Schools</h3><button className="ghost" onClick={load}>Refresh</button></div>
        {loading ? <div className="spinner small" aria-label="Loading schools" />
          : schools.length === 0 ? <p className="empty-state">No schools have been created.</p>
          : schools.map(school => (
            <article className="profile-row school-row" key={school.id}>
              <div className="school-mark">🏫</div>
              <div className="profile-summary">
                <strong>{school.schoolName}</strong>
                <span>{school.schoolCode} · {school.schoolType || 'Type not set'}</span>
                <small>{[school.districtName, school.city, school.stateCode].filter(Boolean).join(' · ') || 'Location not set'} · {school.status}</small>
              </div>
              <div className="row-actions">
                <button className="ghost" onClick={() => edit(school)}>Edit</button>
                <button className="danger" disabled={busy} onClick={() => remove(school)}>Delete</button>
              </div>
            </article>
          ))}
      </div>
    </section>
  )
}

const emptyMembership = {
  userId: '',
  schoolId: '',
  membershipType: 'STUDENT',
  externalPersonId: '',
  membershipStatus: 'ACTIVE',
  startDate: '',
  endDate: '',
}

function MembershipAdmin() {
  const [memberships, setMemberships] = useState([])
  const [users, setUsers] = useState([])
  const [schools, setSchools] = useState([])
  const [form, setForm] = useState(emptyMembership)
  const [editingId, setEditingId] = useState(null)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  async function load() {
    setLoading(true)
    setError('')
    try {
      const [membershipRows, userRows, schoolRows] = await Promise.all([
        api('/api/admin/school-memberships'),
        api('/api/admin/users'),
        api('/api/admin/schools'),
      ])
      setMemberships(membershipRows)
      setUsers(userRows)
      setSchools(schoolRows)
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  function change(name, value) {
    setForm(current => ({ ...current, [name]: value }))
  }

  function resetForm() {
    setEditingId(null)
    setForm(emptyMembership)
    setError('')
  }

  function edit(membership) {
    setEditingId(membership.id)
    setForm({
      userId: String(membership.userId),
      schoolId: String(membership.schoolId),
      membershipType: membership.membershipType,
      externalPersonId: membership.externalPersonId || '',
      membershipStatus: membership.membershipStatus || 'ACTIVE',
      startDate: membership.startDate || '',
      endDate: membership.endDate || '',
    })
    setError('')
    setMessage('')
  }

  async function save(event) {
    event.preventDefault()
    setBusy(true)
    setError('')
    setMessage('')
    const payload = {
      userId: Number(form.userId),
      schoolId: Number(form.schoolId),
      membershipType: form.membershipType,
      externalPersonId: form.externalPersonId || null,
      membershipStatus: form.membershipStatus || null,
      startDate: form.startDate || null,
      endDate: form.endDate || null,
    }
    try {
      if (editingId !== null) {
        await api(`/api/admin/school-memberships/${editingId}`, {
          method: 'PUT',
          body: JSON.stringify(payload),
        })
        setMessage('School membership updated.')
      } else {
        await api('/api/admin/school-memberships', {
          method: 'POST',
          body: JSON.stringify(payload),
        })
        setMessage('School member added.')
      }
      resetForm()
      await load()
    } catch (e) {
      setError(e.message)
    } finally {
      setBusy(false)
    }
  }

  async function remove(membership) {
    if (!window.confirm(`Remove ${membership.userName} from ${membership.schoolName}?`)) return
    setBusy(true)
    setError('')
    setMessage('')
    try {
      await api(`/api/admin/school-memberships/${membership.id}`, { method: 'DELETE' })
      if (editingId === membership.id) resetForm()
      setMessage('School member removed.')
      await load()
    } catch (e) {
      setError(e.message)
    } finally {
      setBusy(false)
    }
  }

  const formUnavailable = users.length === 0 || schools.length === 0

  return (
    <section className="profile-admin membership-admin">
      <div className="section-heading">
        <div>
          <p className="eyebrow">ADMIN ONLY</p>
          <h3>School membership management</h3>
          <p>Add users to schools and manage their membership details.</p>
        </div>
        {editingId !== null && <button className="ghost" onClick={resetForm}>Cancel edit</button>}
      </div>

      {formUnavailable && !loading &&
        <div className="error profile-message">Create at least one user and one school before adding a membership.</div>}

      <form className="profile-form membership-form" onSubmit={save}>
        <label className="field">
          <span>User</span>
          <select required value={form.userId} onChange={event => change('userId', event.target.value)}>
            <option value="">Select a user</option>
            {users.map(user => <option key={user.id} value={user.id}>{user.name} — {user.email}</option>)}
          </select>
        </label>
        <label className="field">
          <span>School</span>
          <select required value={form.schoolId} onChange={event => change('schoolId', event.target.value)}>
            <option value="">Select a school</option>
            {schools.map(school => <option key={school.id} value={school.id}>{school.schoolName} — {school.schoolCode}</option>)}
          </select>
        </label>
        <label className="field">
          <span>Membership type</span>
          <select required value={form.membershipType} onChange={event => change('membershipType', event.target.value)}>
            <option value="STUDENT">Student</option>
            <option value="TEACHER">Teacher</option>
            <option value="ADMINISTRATOR">Administrator</option>
            <option value="STAFF">Staff</option>
            <option value="GUARDIAN">Guardian</option>
          </select>
        </label>
        <Field label="External person ID" type="text" value={form.externalPersonId}
          required={false} onChange={value => change('externalPersonId', value)} />
        <label className="field">
          <span>Membership status</span>
          <select value={form.membershipStatus} onChange={event => change('membershipStatus', event.target.value)}>
            <option value="ACTIVE">Active</option>
            <option value="INACTIVE">Inactive</option>
            <option value="SUSPENDED">Suspended</option>
          </select>
        </label>
        <Field label="Start date" type="date" value={form.startDate}
          required={false} onChange={value => change('startDate', value)} />
        <Field label="End date" type="date" value={form.endDate}
          required={false} onChange={value => change('endDate', value)} />
        <button className="primary full-width" disabled={busy || formUnavailable}>
          {busy ? 'Saving…' : editingId !== null ? 'Update membership' : 'Add school member'}
        </button>
      </form>

      {message && <div className="success profile-message">{message}</div>}
      {error && <div className="error profile-message">{error}</div>}

      <div className="profile-list">
        <div className="list-heading"><h3>School members</h3><button className="ghost" onClick={load}>Refresh</button></div>
        {loading ? <div className="spinner small" aria-label="Loading memberships" />
          : memberships.length === 0 ? <p className="empty-state">No school memberships have been created.</p>
          : memberships.map(membership => (
            <article className="profile-row membership-row" key={membership.id}>
              <div className="member-mark">{membership.userName.charAt(0).toUpperCase()}</div>
              <div className="profile-summary">
                <strong>{membership.userName}</strong>
                <span>{membership.membershipType} at {membership.schoolName}</span>
                <small>{membership.userEmail} · {membership.membershipStatus}{membership.startDate ? ` · From ${membership.startDate}` : ''}</small>
              </div>
              <div className="row-actions">
                <button className="ghost" onClick={() => edit(membership)}>Edit</button>
                <button className="danger" disabled={busy} onClick={() => remove(membership)}>Remove</button>
              </div>
            </article>
          ))}
      </div>
    </section>
  )
}

createRoot(document.getElementById('root')).render(<React.StrictMode><App /></React.StrictMode>)
