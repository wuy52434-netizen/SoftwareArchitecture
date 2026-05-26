import api from './index'

export async function borrowBook(data) {
  return api.post('/borrow', data)
}

export async function returnBook(data) {
  return api.post('/return', data)
}

export async function getMyBorrows(status = 'all') {
  const queryParams = new URLSearchParams()
  if (status !== 'all') {
    queryParams.append('status', status)
  }
  return api.get(`/my-borrows?${queryParams.toString()}`)
}

export async function getAllBorrows(params = {}) {
  const { page = 1, perPage = 20, bookId, userId, status } = params
  const queryParams = new URLSearchParams()
  
  queryParams.append('page', page)
  queryParams.append('per_page', perPage)
  if (bookId) queryParams.append('book_id', bookId)
  if (userId) queryParams.append('user_id', userId)
  if (status) queryParams.append('status', status)
  
  return api.get(`/borrow-records?${queryParams.toString()}`)
}
