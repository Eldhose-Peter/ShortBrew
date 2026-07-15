# ShortBrew REST API Reference 📖

All API requests and responses communicate via JSON. Date-times are formatted in ISO-8601 UTC format.

---

## 🔒 1. Authentication & Security

ShortBrew endpoints use **JWT Access Tokens** for security.
- To access protected endpoints, you must include the `Authorization` header in your requests:
  ```http
  Authorization: Bearer <your_access_token>
  ```
- **Token Lifespan (Defaults)**:
  - Access Token: `3600000 ms` (1 hour)
  - Refresh Token: `604800000 ms` (7 days)

---

## 🚦 2. Rate Limits

Endpoints protected by rate limiting return a `429 Too Many Requests` if the limit is exceeded.

| Endpoint | Limit | Window | Scoped By |
| :--- | :--- | :--- | :--- |
| `POST /api/v1/urls` | 10 requests | 60 seconds | Authenticated User / Client IP |
| `GET /{code}` | 30 requests | 60 seconds | Client IP |

---

## 🧭 3. API Directory

- **Authentication**: `POST /api/v1/auth/signup`, `POST /api/v1/auth/login`, `GET /api/v1/auth/me`
- **URLs**: `POST /api/v1/urls`, `GET /api/v1/urls`, `GET /api/v1/urls/{url_id}`, `PATCH /api/v1/urls/{url_id}`, `DELETE /api/v1/urls/{url_id}`
- **Redirection**: `GET /{code}`
- **Analytics**: `GET /api/v1/analytics/user/summary`, `GET /api/v1/analytics/urls/{url_id}`
- **Health Check**: `GET /api/health`

---

## 🔑 4. Authentication Endpoints

### 4.1 Sign Up
- **Endpoint**: `POST /api/v1/auth/signup`
- **Request Body**:
  ```json
  {
    "email": "user@example.com",
    "password": "strongpassword123",
    "fullName": "Jane Doe"
  }
  ```
- **Success Response (`201 Created`)**:
  ```json
  {
    "access_token": "eyJhbGciOiJIUzI1NiIsIn...",
    "refresh_token": "eyJhbGciOiJIUzI1NiIsIn...",
    "token_type": "bearer"
  }
  ```

### 4.2 Login
- **Endpoint**: `POST /api/v1/auth/login`
- **Request Body**:
  ```json
  {
    "email": "user@example.com",
    "password": "strongpassword123"
  }
  ```
- **Success Response (`200 OK`)**:
  ```json
  {
    "access_token": "eyJhbGciOiJIUzI1NiIsIn...",
    "refresh_token": "eyJhbGciOiJIUzI1NiIsIn...",
    "token_type": "bearer"
  }
  ```

### 4.3 Get Current User Profile
- **Endpoint**: `GET /api/v1/auth/me`
- **Headers**: `Authorization: Bearer <access_token>`
- **Success Response (`200 OK`)**:
  ```json
  {
    "id": "e4b4f8d2-4b72-466d-9b57-61c0282bf8aa",
    "email": "user@example.com",
    "full_name": "Jane Doe",
    "is_active": true,
    "created_at": "2026-07-16T00:00:00Z"
  }
  ```

---

## 🔗 5. URL Management Endpoints

All URLs endpoints require a valid JWT Access Token.

### 5.1 Create Short URL
- **Endpoint**: `POST /api/v1/urls`
- **Headers**: `Authorization: Bearer <access_token>`
- **Request Body**:
  ```json
  {
    "targetUrl": "https://google.com/search?q=very+long+query+string+here",
    "customAlias": "my-google-search", 
    "expiresAt": "2027-01-01T00:00:00Z",
    "title": "Google Search Link"
  }
  ```
  *(Note: `customAlias`, `expiresAt`, and `title` are optional. If no custom alias is provided, the system auto-generates a short code)*
- **Success Response (`201 Created`)**:
  ```json
  {
    "id": 1,
    "short_code": "0xG3a1",
    "custom_alias": "my-google-search",
    "target_url": "https://google.com/search?q=very+long+query+string+here",
    "title": "Google Search Link",
    "created_at": "2026-07-16T00:01:00Z",
    "expires_at": "2027-01-01T00:00:00Z",
    "is_active": true,
    "total_clicks": 0,
    "short_url": "http://localhost:8080/my-google-search"
  }
  ```

### 5.2 List and Search URLs
- **Endpoint**: `GET /api/v1/urls`
- **Headers**: `Authorization: Bearer <access_token>`
- **Query Parameters**:
  - `page`: Page index (default: `1`)
  - `page_size`: Results per page (default: `10`)
  - `search`: Case-insensitive search query matching target URL, title, code, or alias (optional)
  - `is_active`: Filter by active state `true` or `false` (optional)
- **Success Response (`200 OK`)**:
  ```json
  {
    "items": [
      {
        "id": 1,
        "short_code": "0xG3a1",
        "custom_alias": "my-google-search",
        "target_url": "https://google.com/search?q=very+long+query+string+here",
        "title": "Google Search Link",
        "created_at": "2026-07-16T00:01:00Z",
        "expires_at": "2027-01-01T00:00:00Z",
        "is_active": true,
        "total_clicks": 42,
        "short_url": "http://localhost:8080/my-google-search"
      }
    ],
    "total": 1,
    "page": 1,
    "page_size": 10,
    "total_pages": 1
  }
  ```

### 5.3 Get URL Details
- **Endpoint**: `GET /api/v1/urls/{url_id}`
- **Headers**: `Authorization: Bearer <access_token>`
- **Success Response (`200 OK`)**:
  ```json
  {
    "id": 1,
    "short_code": "0xG3a1",
    "custom_alias": "my-google-search",
    "target_url": "https://google.com/search?q=very+long+query+string+here",
    "title": "Google Search Link",
    "created_at": "2026-07-16T00:01:00Z",
    "expires_at": "2027-01-01T00:00:00Z",
    "is_active": true,
    "total_clicks": 42,
    "short_url": "http://localhost:8080/my-google-search"
  }
  ```

### 5.4 Update URL
- **Endpoint**: `PATCH /api/v1/urls/{url_id}`
- **Headers**: `Authorization: Bearer <access_token>`
- **Request Body** *(All fields optional)*:
  ```json
  {
    "targetUrl": "https://bing.com",
    "expiresAt": "2028-12-31T23:59:59Z",
    "isActive": false,
    "title": "Bing Search Link"
  }
  ```
- **Success Response (`200 OK`)**:
  ```json
  {
    "id": 1,
    "short_code": "0xG3a1",
    "custom_alias": "my-google-search",
    "target_url": "https://bing.com",
    "title": "Bing Search Link",
    "created_at": "2026-07-16T00:01:00Z",
    "expires_at": "2028-12-31T23:59:59Z",
    "is_active": false,
    "total_clicks": 42,
    "short_url": "http://localhost:8080/my-google-search"
  }
  ```

### 5.5 Delete URL
- **Endpoint**: `DELETE /api/v1/urls/{url_id}`
- **Headers**: `Authorization: Bearer <access_token>`
- **Success Response (`204 No Content`)**: No response body.

---

## ⚡️ 6. Redirection Endpoint

### 6.1 Redirect to target URL
- **Endpoint**: `GET /{code}`
- **Path Parameter**: `code` (Either auto-generated `short_code` or `custom_alias`)
- **Success Response (`302 Found`)**:
  - `Location` header is set to the destination target URL.
- **Fail Responses**:
  - `404 Not Found`: If the code/alias does not match any URL record.
  - `410 Gone`: If the matched URL is deactivated or has expired.

---

## 📊 7. Analytics Endpoints

Analytics endpoints require a valid JWT Access Token.

### 7.1 User Dashboard Summary
Retrieves aggregate stats for the active user.
- **Endpoint**: `GET /api/v1/analytics/user/summary`
- **Headers**: `Authorization: Bearer <access_token>`
- **Success Response (`200 OK`)**:
  ```json
  {
    "total_urls": 15,
    "total_clicks": 3520,
    "clicks_today": 128,
    "top_urls": [
      {
        "short_code": "my-google-search",
        "target_url": "https://google.com",
        "title": "Google Link",
        "total_clicks": 1420
      }
    ],
    "daily_clicks": [
      {
        "stat_date": "2026-07-15",
        "click_count": 105
      },
      {
        "stat_date": "2026-07-16",
        "click_count": 128
      }
    ]
  }
  ```

### 7.2 Detailed URL Analytics
Retrieves time-series and breakdown stats for a specific URL.
- **Endpoint**: `GET /api/v1/analytics/urls/{url_id}`
- **Headers**: `Authorization: Bearer <access_token>`
- **Query Parameter**:
  - `days`: Time window in days (default: `30`)
- **Success Response (`200 OK`)**:
  ```json
  {
    "short_code": "my-google-search",
    "total_clicks": 1420,
    "daily_clicks": [
      {
        "stat_date": "2026-07-15",
        "click_count": 52
      },
      {
        "stat_date": "2026-07-16",
        "click_count": 78
      }
    ],
    "top_referrers": [
      {
        "referrer": "direct",
        "count": 920
      },
      {
        "referrer": "https://twitter.com",
        "count": 500
      }
    ],
    "top_countries": [
      {
        "country_code": "US",
        "count": 800
      },
      {
        "country_code": "IN",
        "count": 620
      }
    ]
  }
  ```

---

## 🏥 8. System Health Check

### 8.1 Health Status
- **Endpoint**: `GET /api/health`
- **Success Response (`200 OK`)** *(Overall healthy)*:
  ```json
  {
    "status": "UP",
    "database": {
      "status": "UP",
      "details": "Database is responsive"
    },
    "redis": {
      "status": "UP",
      "details": "Redis is responsive (PONG)"
    }
  }
  ```
- **Failure Response (`503 Service Unavailable`)** *(If DB or Redis goes down)*:
  ```json
  {
    "status": "DOWN",
    "database": {
      "status": "DOWN",
      "details": "Database connection error: Connection refused"
    },
    "redis": {
      "status": "UP",
      "details": "Redis is responsive (PONG)"
    }
  }
  ```

---

## ⚠️ 9. Common Error Payload Schema

For client error responses (`400`, `401`, `404`, `409`, `410`, `429`), the payload follows this standard schema:

```json
{
  "error": "Error description details go here."
}
```
