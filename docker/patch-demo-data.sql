SET NAMES utf8mb4;
USE library;

-- 非破坏性演示数据补丁：不清空已有数据，只补齐自助借书必需的读者账号和图书副本。

INSERT INTO `user` (username, password, real_name, user_type, gender, phone, email, category_id, status)
SELECT
    'user1',
    '$2b$10$GLGWOlSFJbzOMCpxWtmR8uiwfD2A5ARE81YKS6i1qIVJX0rPJj0Qq',
    CONVERT(UNHEX('E699AEE9809AE8AFBBE88085') USING utf8mb4),
    'reader',
    CONVERT(UNHEX('E5A5B3') USING utf8mb4),
    '13900000000',
    'reader@example.com',
    COALESCE((SELECT category_id FROM reader_category ORDER BY category_id LIMIT 1), NULL),
    'active'
WHERE NOT EXISTS (
    SELECT 1 FROM `user` WHERE username = 'user1'
);

UPDATE `user`
SET
    real_name = CONVERT(UNHEX('E699AEE9809AE8AFBBE88085') USING utf8mb4),
    gender = CONVERT(UNHEX('E5A5B3') USING utf8mb4),
    updated_at = NOW()
WHERE username = 'user1' AND (real_name LIKE '%?%' OR gender LIKE '%?%');

INSERT INTO book_copy (book_id, barcode, location_id, status, book_condition)
SELECT
    bi.id,
    CONCAT('BC', LPAD(bi.id, 4, '0'), LPAD(n.n, 3, '0')),
    COALESCE(
        (
            SELECT l.location_id
            FROM location l
            ORDER BY
                CASE
                    WHEN bi.category_id = 2 AND l.location_name LIKE CONCAT('%', CONVERT(UNHEX('E58E86E58FB2') USING utf8mb4), '%') THEN 0
                    WHEN bi.category_id = 3 AND l.location_name LIKE CONCAT('%', CONVERT(UNHEX('E7A791E68A80') USING utf8mb4), '%') THEN 0
                    WHEN bi.category_id = 4 AND l.location_name LIKE CONCAT('%', CONVERT(UNHEX('E889BAE69CAF') USING utf8mb4), '%') THEN 0
                    WHEN bi.category_id = 5 AND l.location_name LIKE CONCAT('%', CONVERT(UNHEX('E69599E882B2') USING utf8mb4), '%') THEN 0
                    WHEN bi.category_id = 1 AND l.location_name LIKE CONCAT('%', CONVERT(UNHEX('E69687E5ADA6') USING utf8mb4), '%') THEN 0
                    ELSE 1
                END,
                l.location_id
            LIMIT 1
        ),
        1
    ) AS location_id,
    CASE WHEN n.n <= bi.available_copies THEN 'available' ELSE 'borrowed' END AS status,
    'new'
FROM book_info bi
JOIN (
    SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
    UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
    UNION ALL SELECT 11 UNION ALL SELECT 12 UNION ALL SELECT 13 UNION ALL SELECT 14 UNION ALL SELECT 15
    UNION ALL SELECT 16 UNION ALL SELECT 17 UNION ALL SELECT 18 UNION ALL SELECT 19 UNION ALL SELECT 20
) n ON n.n <= bi.total_copies
WHERE NOT EXISTS (
    SELECT 1 FROM book_copy bc WHERE bc.book_id = bi.id
);
