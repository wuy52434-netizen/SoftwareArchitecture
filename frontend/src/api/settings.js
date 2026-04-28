import api from './index'

export async function getSettings() {
  return api.get('/settings')
}

export async function updateSettings(data) {
  return api.put('/settings', data)
}
