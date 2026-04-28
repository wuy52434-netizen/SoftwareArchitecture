import api from './index'

export async function getUsers() {
  return api.get('/users')
}

export async function addUser(data) {
  return api.post('/users', data)
}

export async function deleteUser(id) {
  return api.delete(`/users/${id}`)
}
