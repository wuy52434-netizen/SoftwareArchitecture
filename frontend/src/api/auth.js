import api from './index'

export async function login(username, password) {
  return api.post('/auth/login', { username, password })
}

export async function logout() {
  return api.post('/auth/logout')
}

export async function register(data) {
  return api.post('/auth/register', data)
}

export async function refreshToken(refreshToken) {
  return api.post('/auth/refresh', { refreshToken })
}

export async function getCurrentUser() {
  return api.get('/users/current')
}
