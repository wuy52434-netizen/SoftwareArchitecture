package com.library.borrow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.library.borrow.entity.BorrowRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BorrowRecordMapper extends BaseMapper<BorrowRecord> {

    @Select("SELECT * FROM borrow_record WHERE user_id = #{userId} AND return_date IS NULL AND deleted = 0")
    List<BorrowRecord> selectActiveByUserId(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM borrow_record WHERE user_id = #{userId} AND return_date IS NULL AND deleted = 0")
    int countActiveByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM borrow_record WHERE record_id = #{recordId} AND deleted = 0")
    BorrowRecord selectById(@Param("recordId") Long recordId);

    @Select("SELECT br.* FROM borrow_record br " +
            "JOIN book_copy bc ON br.copy_id = bc.copy_id " +
            "JOIN book_info bi ON bc.book_id = bi.id " +
            "WHERE bi.id = #{bookId} AND br.return_date IS NULL AND br.deleted = 0 " +
            "LIMIT 1")
    BorrowRecord selectActiveByBookId(@Param("bookId") Long bookId);
}
