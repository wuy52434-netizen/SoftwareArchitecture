"""API 契约 (JSON Schema) 定义。

契约即接口双方共同遵守的"协议"。这里用 JSON Schema 固化响应结构，
消费方(前端/借书机)与服务提供方(后端)都依赖它，形成消费者驱动契约。
"""

# 统一响应外壳：code / message / data / timestamp
RESULT_SCHEMA = {
    "type": "object",
    "required": ["code", "message", "data", "timestamp"],
    "properties": {
        "code": {"type": "integer"},
        "message": {"type": "string"},
        "data": {},
        "timestamp": {"type": "integer"},
    },
    "additionalProperties": True,
}

# 图书对象契约
BOOK_SCHEMA = {
    "type": "object",
    "required": ["id", "isbn", "title", "author", "status", "availableCopies"],
    "properties": {
        "id": {"type": "integer"},
        "isbn": {"type": "string"},
        "title": {"type": "string"},
        "author": {"type": "string"},
        "price": {"type": ["number", "null"]},
        "category": {"type": "string"},
        "status": {"type": "string", "enum": ["available", "borrowed", "frozen"]},
        "totalCopies": {"type": "integer"},
        "availableCopies": {"type": "integer"},
        "publishDate": {"type": ["integer", "null"]},
    },
    "additionalProperties": True,
}

# 图书分页列表
BOOKS_PAGE_SCHEMA = {
    "type": "object",
    "required": ["records", "total", "size", "current"],
    "properties": {
        "records": {"type": "array", "items": BOOK_SCHEMA},
        "total": {"type": "integer"},
        "size": {"type": "integer"},
        "current": {"type": "integer"},
    },
}

# 登录响应
LOGIN_SCHEMA = {
    "type": "object",
    "required": ["accessToken", "refreshToken", "tokenType", "expiresIn", "user"],
    "properties": {
        "accessToken": {"type": "string"},
        "refreshToken": {"type": "string"},
        "tokenType": {"type": "string"},
        "expiresIn": {"type": "integer"},
        "user": {"type": "object"},
    },
}

# 分类列表
CATEGORY_SCHEMA = {
    "type": "array",
    "items": {
        "type": "object",
        "required": ["categoryId", "categoryName"],
        "properties": {
            "categoryId": {"type": "integer"},
            "categoryName": {"type": "string"},
        },
    },
}