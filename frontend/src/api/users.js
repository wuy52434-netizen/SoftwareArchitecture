import api from './index'

export async function getUsers() {
  return api.get('/users')
}

export async function getUserByCard(cardNo) {
  return api.get(`/users/card/${encodeURIComponent(cardNo)}`)
}

export async function addUser(data) {
  return api.post('/users', data)
}

export async function updateUser(id, data) {
  return api.put(`/users/${id}`, data)
}

export async function deleteUser(id) {
  return api.delete(`/users/${id}`)
}
