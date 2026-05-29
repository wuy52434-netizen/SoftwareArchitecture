import api from './index'

const categoryMap = {
  '文学': 1,
  '历史': 2,
  '科技': 3,
  '计算机': 3,
  '艺术': 4,
  '教育': 5,
  '其他': 6
}

export async function getBooks(params = {}) {
  const { page = 1, perPage = 24, search, category, language, year, status } = params
  const queryParams = new URLSearchParams()
  
  queryParams.append('page', page)
  queryParams.append('per_page', perPage)
  if (search) queryParams.append('search', search)
  if (category) queryParams.append('category', categoryMap[category] || category)
  if (language) queryParams.append('language', language)
  if (year) queryParams.append('year', year)
  if (status) queryParams.append('status', status)
  
  return api.get(`/books?${queryParams.toString()}`)
}

export async function getBook(id) {
  return api.get(`/books/${id}`)
}

export async function scanBook(code) {
  return api.get(`/books/scan?code=${encodeURIComponent(code)}`)
}

export async function getCopyByBarcode(barcode) {
  return api.get(`/books/copy/barcode/${encodeURIComponent(barcode)}`)
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
  return api.get('/books/popular')
}

export async function getNewBooks() {
  return api.get('/books/newest')
}
