package com.library.book.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.library.book.entity.BookCopy;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BookCopyMapper extends BaseMapper<BookCopy> {
}
