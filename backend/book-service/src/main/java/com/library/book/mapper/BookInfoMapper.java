package com.library.book.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.library.book.entity.BookInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BookInfoMapper extends BaseMapper<BookInfo> {

    @Select("SELECT * FROM book_info WHERE isbn = #{isbn} AND deleted = 0")
    BookInfo selectByIsbn(@Param("isbn") String isbn);

    @Select("SELECT * FROM book_info WHERE id = #{id} AND deleted = 0")
    BookInfo selectById(@Param("id") Long id);

    @Select("<script>" +
            "SELECT * FROM book_info WHERE deleted = 0 " +
            "<if test='search != null and search != \"\"'>" +
            " AND (title LIKE CONCAT('%', #{search}, '%') " +
            " OR author LIKE CONCAT('%', #{search}, '%') " +
            " OR isbn LIKE CONCAT('%', #{search}, '%'))" +
            "</if>" +
            "<if test='categoryId != null'>" +
            " AND category_id = #{categoryId}" +
            "</if>" +
            "<if test='status != null and status != \"\"'>" +
            " AND status = #{status}" +
            "</if>" +
            " ORDER BY created_at DESC" +
            "</script>")
    List<BookInfo> searchBooks(@Param("search") String search, 
                                @Param("categoryId") Long categoryId,
                                @Param("status") String status);
}
