package com.library.search.controller;

import com.library.common.result.Result;
import com.library.search.dto.SearchDTO.*;
import com.library.search.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "搜索接口")
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @Operation(summary = "图书搜索")
    @GetMapping
    public Result<SearchResult> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        
        SearchRequest request = new SearchRequest();
        request.setKeyword(keyword);
        request.setCategory(category);
        request.setYear(year);
        request.setStatus(status);
        request.setPage(page);
        request.setSize(size);
        
        SearchResult result = searchService.search(request);
        return Result.success(result);
    }

    @Operation(summary = "热门图书")
    @GetMapping("/hot")
    public Result<List<BookItem>> getHotBooks(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "10") Integer size) {
        
        List<BookItem> books = searchService.getHotBooks(category, size);
        return Result.success(books);
    }

    @Operation(summary = "推荐图书")
    @GetMapping("/recommend")
    public Result<List<BookItem>> getRecommendBooks(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "10") Integer size) {
        
        List<BookItem> books = searchService.getHotBooks(null, size);
        return Result.success(books);
    }
}
