package com.library.stats.client;

import com.library.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 图书服务客户端 —— 用于把看板统计口径对齐到权威数据源（KNWN-DEF-01）。
 *
 * <p>修复前的实现是 {@code getCounter("total:books", 24L)}：读一个本地 Redis 计数器，
 * 初值还是硬编码的 24。于是不管库里实际有多少本书，看板都从 24 开始数，
 * 与 {@code GET /api/books} 返回的 total 天然对不上（实测 24 vs 25）。
 * 统计数字必须来自唯一权威源，而不是另一份会漂移的副本。
 */
@FeignClient(name = "book-service", fallback = BookClientFallback.class)
public interface BookClient {

    /**
     * 分页查询图书。只关心 {@code data.total}，所以 per_page 传 1 即可。
     *
     * @param status 可传 "available" 统计在架数量；不传表示全部
     */
    @GetMapping("/api/books")
    Result<BookPage> listBooks(@RequestParam("page") int page,
                               @RequestParam("per_page") int perPage,
                               @RequestParam(value = "status", required = false) String status);

    /** 只取分页元信息里的 total，避免为一个计数拉回整页数据。 */
    class BookPage {
        private Long total;

        public Long getTotal() { return total; }
        public void setTotal(Long total) { this.total = total; }
    }
}
