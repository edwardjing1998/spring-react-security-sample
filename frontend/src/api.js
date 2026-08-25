const TOKEN_KEY = 'security_sample_access_token'

export const tokenStore = {
  get: () => sessionStorage.getItem(TOKEN_KEY),
  set: (token) => sessionStorage.setItem(TOKEN_KEY, token),
  clear: () => sessionStorage.removeItem(TOKEN_KEY),
}

export async function api(path, options = {}) {
  const token = tokenStore.get()
  const response = await fetch(path, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
  })

  const body = response.status === 204 ? null : await response.json().catch(() => null)
  if (!response.ok) {
    const error = new Error(body?.message || `Request failed (${response.status})`)
    error.status = response.status
    error.fieldErrors = body?.fieldErrors || {}
    throw error
  }
  return body
}

