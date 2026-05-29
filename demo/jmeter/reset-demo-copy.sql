SET @demo_barcode := 'BC0024001';
SET @demo_copy_id := (
  SELECT copy_id
  FROM book_copy
  WHERE barcode = @demo_barcode AND deleted = 0
  LIMIT 1
);
SET @demo_book_id := (
  SELECT book_id
  FROM book_copy
  WHERE copy_id = @demo_copy_id
  LIMIT 1
);
SET @demo_user_id := (
  SELECT user_id
  FROM `user`
  WHERE username = 'user1' AND deleted = 0
  LIMIT 1
);

UPDATE borrow_record
SET return_date = CURDATE(),
    status = 'returned',
    updated_at = NOW()
WHERE copy_id = @demo_copy_id
  AND return_date IS NULL
  AND deleted = 0;

UPDATE book_copy
SET status = 'available',
    updated_at = NOW()
WHERE copy_id = @demo_copy_id;

UPDATE book_info bi
SET available_copies = (
      SELECT COUNT(*)
      FROM book_copy bc
      WHERE bc.book_id = bi.id
        AND bc.status = 'available'
        AND bc.deleted = 0
    ),
    status = CASE
      WHEN (
        SELECT COUNT(*)
        FROM book_copy bc
        WHERE bc.book_id = bi.id
          AND bc.status = 'available'
          AND bc.deleted = 0
      ) > 0 THEN 'available'
      ELSE 'borrowed'
    END,
    updated_at = NOW()
WHERE bi.id = @demo_book_id;

SELECT @demo_user_id AS user_id,
       @demo_book_id AS book_id,
       @demo_copy_id AS copy_id,
       @demo_barcode AS barcode;
