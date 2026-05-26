package com.library.book.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.book.dto.BookDTO.*;
import com.library.book.entity.BookCategory;
import com.library.book.entity.BookInfo;
import com.library.book.service.BookService;
import com.library.common.result.PageResult;
import com.library.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "图书管理接口")
@RestController
@RequestMapping("")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @Operation(summary = "获取图书列表")
    @GetMapping("/books")
    public Result<PageResult<BookResponse>> listBooks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "24") int per_page,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long category,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String status) {

        Page<BookInfo> pageResult = bookService.pageBooks(page, per_page, search, category, status);

        List<BookResponse> books = pageResult.getRecords().stream()
                .map(bookService::toBookResponse)
                .collect(Collectors.toList());

        PageResult<BookResponse> result = PageResult.of(
                books,
                pageResult.getTotal(),
                pageResult.getSize(),
                pageResult.getCurrent()
        );

        return Result.success(result);
    }

    @Operation(summary = "获取图书详情")
    @GetMapping("/books/{id}")
    public Result<BookResponse> getBookById(@PathVariable Long id) {
        BookInfo book = bookService.getById(id);
        return Result.success(bookService.toBookResponse(book));
    }

    @Operation(summary = "创建图书")
    @PostMapping("/books")
    public Result<BookResponse> createBook(@Valid @RequestBody BookCreateRequest request) {
        BookInfo book = bookService.createBook(request);
        return Result.success("创建成功", bookService.toBookResponse(book));
    }

    @Operation(summary = "更新图书")
    @PutMapping("/books/{id}")
    public Result<BookResponse> updateBook(
            @PathVariable Long id,
            @Valid @RequestBody BookUpdateRequest request) {
        BookInfo book = bookService.updateBook(id, request);
        return Result.success("更新成功", bookService.toBookResponse(book));
    }

    @Operation(summary = "删除图书")
    @DeleteMapping("/books/{id}")
    public Result<Void> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return Result.success("删除成功");
    }

    @Operation(summary = "更新图书状态")
    @PutMapping("/books/{id}/status")
    public Result<BookResponse> updateBookStatus(
            @PathVariable Long id,
            @RequestBody BookUpdateRequest request) {
        BookInfo book = bookService.getById(id);
        bookService.updateBook(id, request);
        return Result.success("状态更新成功", bookService.toBookResponse(book));
    }

    @Operation(summary = "获取热门图书")
    @GetMapping("/books/popular")
    public Result<List<BookResponse>> getPopularBooks() {
        Page<BookInfo> pageResult = bookService.pageBooks(1, 8, null, null, null);
        List<BookResponse> books = pageResult.getRecords().stream()
                .map(bookService::toBookResponse)
                .collect(Collectors.toList());
        return Result.success(books);
    }

    @Operation(summary = "获取新书")
    @GetMapping("/books/newest")
    public Result<List<BookResponse>> getNewBooks() {
        Page<BookInfo> pageResult = bookService.pageBooks(1, 8, null, null, null);
        List<BookResponse> books = pageResult.getRecords().stream()
                .map(bookService::toBookResponse)
                .collect(Collectors.toList());
        return Result.success(books);
    }

    @Operation(summary = "获取分类列表")
    @GetMapping("/books/categories")
    public Result<List<CategoryResponse>> getCategories() {
        List<CategoryResponse> categories = List.of(
                createCategory(1L, "文学", "文学作品"),
                createCategory(2L, "计算机", "计算机技术"),
                createCategory(3L, "历史", "历史书籍"),
                createCategory(4L, "艺术", "艺术欣赏"),
                createCategory(5L, "教育", "教育理论"),
                createCategory(6L, "科幻", "科幻小说"),
                createCategory(7L, "哲学", "哲学思想"),
                createCategory(8L, "经济", "经济学")
        );
        return Result.success(categories);
    }

    private CategoryResponse createCategory(Long id, String name, String description) {
        CategoryResponse category = new CategoryResponse();
        category.setCategoryId(id);
        category.setCategoryName(name);
        category.setDescription(description);
        return category;
    }
}