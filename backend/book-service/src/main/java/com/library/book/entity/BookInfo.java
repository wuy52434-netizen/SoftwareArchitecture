package com.library.book.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("book_info")
public class BookInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String isbn;

    private String title;

    private String author;

    private Long publisherId;

    private BigDecimal price;

    private Long categoryId;

    private String summary;

    private LocalDate publicationDate;

    private String coverImage;

    private Integer totalCopies;

    private Integer availableCopies;

    private String status;

    private String language;

    private String description;

    private String location;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
