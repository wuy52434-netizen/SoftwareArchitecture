package com.library.book.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.library.book.entity.BookInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

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

    @Update("UPDATE book_info " +
            "SET status = CASE " +
            "  WHEN available_copies + #{change} <= 0 THEN #{borrowedStatus} " +
            "  WHEN status = #{borrowedStatus} AND available_copies + #{change} > 0 THEN #{availableStatus} " +
            "  ELSE status END, " +
            "available_copies = available_copies + #{change}, " +
            "updated_at = NOW() " +
            "WHERE id = #{bookId} AND deleted = 0 AND available_copies + #{change} >= 0")
    int updateAvailableCopies(@Param("bookId") Long bookId,
                              @Param("change") int change,
                              @Param("availableStatus") String availableStatus,
                              @Param("borrowedStatus") String borrowedStatus);
}
