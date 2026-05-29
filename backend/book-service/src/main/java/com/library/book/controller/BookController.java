package com.library.book.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.book.dto.BookDTO.*;
import com.library.book.entity.BookCopy;
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
@RequestMapping("/api")
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
            @RequestParam(required = false) String year,
            @RequestParam(required = false) String status) {

        Page<BookInfo> pageResult = bookService.pageBooks(page, per_page, search, category, language, year, status);

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
        return Result.success();
    }

    @Operation(summary = "更新图书状态")
    @PutMapping("/books/{id}/status")
    public Result<BookResponse> updateBookStatus(
            @PathVariable Long id,
            @RequestBody BookUpdateRequest request) {
        BookInfo book = bookService.updateBook(id, request);
        return Result.success("状态更新成功", bookService.toBookResponse(book));
    }

    @Operation(summary = "扣减库存")
    @PutMapping("/books/{id}/decrease-stock")
    public Result<Void> decreaseStock(@PathVariable Long id, @RequestParam(required = false) Long copyId) {
        bookService.decreaseStock(id, copyId);
        return Result.success();
    }

    @Operation(summary = "增加库存")
    @PutMapping("/books/{id}/increase-stock")
    public Result<Void> increaseStock(@PathVariable Long id, @RequestParam(required = false) Long copyId) {
        bookService.increaseStock(id, copyId);
        return Result.success();
    }

    @Operation(summary = "根据副本ID获取副本")
    @GetMapping("/books/copy/{copyId}")
    public Result<BookCopy> getCopyById(@PathVariable Long copyId) {
        BookCopy copy = bookService.getCopyById(copyId);
        return Result.success(copy);
    }

    @Operation(summary = "根据副本条码获取副本")
    @GetMapping("/books/copy/barcode/{barcode}")
    public Result<CopyResponse> getCopyByBarcode(@PathVariable String barcode) {
        BookCopy copy = bookService.getCopyByBarcode(barcode);
        BookInfo book = bookService.getById(copy.getBookId());
        return Result.success(bookService.toCopyResponse(copy, book));
    }

    @Operation(summary = "借书机扫码查询图书")
    @GetMapping("/books/scan")
    public Result<ScanBookResponse> scanBook(@RequestParam String code) {
        return Result.success(bookService.scanBook(code));
    }

    @Operation(summary = "获取图书可用副本")
    @GetMapping("/books/{id}/available-copy")
    public Result<BookCopy> getAvailableCopy(@PathVariable Long id) {
        BookCopy copy = bookService.getAvailableCopyByBookId(id);
        return Result.success(copy);
    }

    @Operation(summary = "更新副本状态")
    @PutMapping("/books/copy/{copyId}/status")
    public Result<Void> updateCopyStatus(@PathVariable Long copyId, @RequestParam String status) {
        bookService.updateCopyStatus(copyId, status);
        return Result.success();
    }

    @Operation(summary = "获取热门图书")
    @GetMapping("/books/popular")
    public Result<List<BookResponse>> getPopularBooks() {
        Page<BookInfo> pageResult = bookService.pageBooks(1, 8, null, null, null, null, null);
        List<BookResponse> books = pageResult.getRecords().stream()
                .map(bookService::toBookResponse)
                .collect(Collectors.toList());
        return Result.success(books);
    }

    @Operation(summary = "获取新书")
    @GetMapping("/books/newest")
    public Result<List<BookResponse>> getNewBooks() {
        Page<BookInfo> pageResult = bookService.pageBooks(1, 8, null, null, null, null, null);
        List<BookResponse> books = pageResult.getRecords().stream()
                .map(bookService::toBookResponse)
                .collect(Collectors.toList());
        return Result.success(books);
    }

    @Operation(summary = "获取分类列表")
    @GetMapping({"/books/categories", "/categories"})
    public Result<List<CategoryResponse>> getCategories() {
        List<CategoryResponse> categories = List.of(
                createCategory(1L, "文学", "文学作品"),
                createCategory(2L, "历史", "历史书籍"),
                createCategory(3L, "科技", "科学技术"),
                createCategory(4L, "艺术", "艺术欣赏"),
                createCategory(5L, "教育", "教育理论"),
                createCategory(6L, "其他", "其他类别")
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
