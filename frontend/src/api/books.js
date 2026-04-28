import api from './index'

export async function getBooks(params = {}) {
  const { page = 1, perPage = 24, search, category, language, year } = params
  const queryParams = new URLSearchParams()
  
  queryParams.append('page', page)
  queryParams.append('per_page', perPage)
  if (search) queryParams.append('search', search)
  if (category) queryParams.append('category', category)
  if (language) queryParams.append('language', language)
  if (year) queryParams.append('year', year)
  
  return api.get(`/books?${queryParams.toString()}`)
}

export async function getBook(id) {
  return api.get(`/books/${id}`)
}

export async function addBook(data) {
  return api.post('/books', data)
}

export async function updateBook(id, data) {
  return api.put(`/books/${id}`, data)
}

export async function deleteBook(id) {
  return api.delete(`/books/${id}`)
}

export async function updateBookStatus(id, status) {
  return api.put(`/books/${id}/status`, { status })
}

export async function getPopularBooks() {
  return api.get('/books?popular=true&per_page=8')
}

export async function getNewBooks() {
  return api.get('/books?sort=newest&per_page=8')
}
