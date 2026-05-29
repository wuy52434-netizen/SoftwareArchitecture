import api from './index'

export async function borrowBook(data) {
  return api.post('/borrow', data)
}

export async function returnBook(data) {
  return api.post('/return', data)
}

export async function getMyBorrows(status = 'all') {
  const queryParams = new URLSearchParams()
  const currentUser = getCurrentUser()
  const userId = currentUser?.userId || currentUser?.id
  if (userId) {
    queryParams.append('userId', userId)
  }
  if (status !== 'all') {
    queryParams.append('status', status)
  }
  return api.get(`/my-borrows?${queryParams.toString()}`)
}

export async function getAllBorrows(params = {}) {
  const { page = 1, perPage = 20, bookId, userId, status } = params
  const queryParams = new URLSearchParams()
  
  queryParams.append('page', page)
  queryParams.append('size', perPage)
  if (bookId) queryParams.append('bookId', bookId)
  if (userId) queryParams.append('userId', userId)
  if (status) queryParams.append('status', status)
  
  return api.get(`/borrow-records?${queryParams.toString()}`)
}

export async function renewBook(recordId, days) {
  const queryParams = new URLSearchParams()
  if (days) {
    queryParams.append('days', days)
  }
  const query = queryParams.toString()
  return api.put(`/borrow-records/${recordId}/renew${query ? `?${query}` : ''}`)
}

function getCurrentUser() {
  const savedUser = localStorage.getItem('currentUser')
  if (!savedUser) return null
  try {
    return JSON.parse(savedUser)
  } catch (e) {
    return null
  }
}
