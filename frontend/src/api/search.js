import api from './index'

/**
 * Elasticsearch 全文检索客户端
 * 对应后端 search-service: GET /api/search
 * 返回结构: { books:[{id,title,author,publisher,category,summary,
 *                     highlightTitle,highlightAuthor,highlightSummary,
 *                     price,coverUrl,status,availableCopies,publishDate}],
 *            total, page, size, pages, categoryAggs, yearAggs }
 */
export async function searchBooks(params = {}) {
  const { keyword, category, page = 1, size = 24, status } = params
  const q = new URLSearchParams()
  if (keyword) q.append('keyword', keyword)
  if (category) q.append('category', category)
  if (status) q.append('status', status)
  q.append('page', page)
  q.append('size', size)
  return api.get(`/search?${q.toString()}`)
}

/** 热门图书（按借阅量/热度排序） */
export async function searchHot(size = 10) {
  return api.get(`/search/hot?size=${size}`)
}

/** 个性化推荐 */
export async function searchRecommend(size = 10) {
  return api.get(`/search/recommend?size=${size}`)
}

/** 分类聚合统计（sidebar 用） */
export async function searchAggs() {
  return api.get('/search/aggs')
}