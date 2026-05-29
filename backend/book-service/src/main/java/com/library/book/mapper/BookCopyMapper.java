package com.library.book.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.library.book.entity.BookCopy;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface BookCopyMapper extends BaseMapper<BookCopy> {

    @Select("SELECT * FROM book_copy WHERE book_id = #{bookId} AND status = 'available' AND deleted = 0 ORDER BY copy_id ASC LIMIT 1")
    BookCopy selectFirstAvailableByBookId(@Param("bookId") Long bookId);

    @Select("SELECT * FROM book_copy WHERE barcode = #{barcode} AND deleted = 0 LIMIT 1")
    BookCopy selectByBarcode(@Param("barcode") String barcode);

    @Select("SELECT COUNT(*) FROM borrow_record WHERE book_id = #{bookId} AND return_date IS NULL AND deleted = 0")
    int countActiveBorrowsByBookId(@Param("bookId") Long bookId);

    @Select("SELECT location_id FROM location WHERE location_name = #{locationName} ORDER BY location_id LIMIT 1")
    Long selectLocationIdByName(@Param("locationName") String locationName);

    @Select("SELECT location_id FROM location ORDER BY location_id LIMIT 1")
    Long selectFirstLocationId();

    @Update("UPDATE book_copy SET status = #{newStatus}, updated_at = NOW() " +
            "WHERE copy_id = #{copyId} AND book_id = #{bookId} AND status = #{expectedStatus} AND deleted = 0")
    int updateStatusIfCurrent(@Param("copyId") Long copyId,
                              @Param("bookId") Long bookId,
                              @Param("expectedStatus") String expectedStatus,
                              @Param("newStatus") String newStatus);
}
