# BookSwap Hub - ER Diagram (with Payment Model)

```mermaid
erDiagram
    USERS {
        BIGINT id PK
        VARCHAR(50) username UK
        VARCHAR(100) email UK
        VARCHAR(255) password
        VARCHAR(50) role
        DATETIME created_at
    }

    BOOKS {
        BIGINT id PK
        VARCHAR(200) title
        VARCHAR(150) author
        TEXT description
        DECIMAL(10,2) price
        VARCHAR(255) image_path
        BYTEA image_data
        VARCHAR(100) image_type
        VARCHAR(50) category
        VARCHAR(50) condition
        DOUBLE latitude
        DOUBLE longitude
        VARCHAR(500) address
        BOOLEAN sold
        BIGINT owner_id FK
        DATETIME created_at
    }

    PURCHASE_REQUESTS {
        BIGINT id PK
        BIGINT book_id FK
        BIGINT requester_id FK
        VARCHAR(500) message
        VARCHAR(20) status
        DATETIME created_at
    }

    CHAT_MESSAGES {
        BIGINT id PK
        BIGINT request_id FK
        BIGINT sender_id FK
        VARCHAR(1000) content
        DATETIME sent_at
    }

    WISHLISTS {
        BIGINT id PK
        BIGINT user_id FK
        BIGINT book_id FK
        DATETIME saved_at
    }

    NOTIFICATIONS {
        BIGINT id PK
        BIGINT recipient_id FK
        VARCHAR(500) message
        BOOLEAN read
        DATETIME created_at
    }

    SALE_RECORDS {
        BIGINT id PK
        BIGINT request_id FK UK
        BIGINT book_id FK
        BIGINT owner_id FK
        BIGINT buyer_id FK
        DECIMAL(10,2) amount
        VARCHAR(20) payment_mode
        VARCHAR(500) notes
        DATETIME received_at
    }

    PAYMENTS {
        BIGINT id PK
        BIGINT sale_record_id FK
        BIGINT payer_id FK
        BIGINT payee_id FK
        DECIMAL(10,2) amount
        VARCHAR(10) currency
        VARCHAR(30) method
        VARCHAR(30) provider
        VARCHAR(30) status
        VARCHAR(100) transaction_ref UK
        DATETIME paid_at
        DATETIME created_at
    }

    USERS ||--o{ BOOKS : owns
    USERS ||--o{ PURCHASE_REQUESTS : creates
    BOOKS ||--o{ PURCHASE_REQUESTS : receives

    PURCHASE_REQUESTS ||--o{ CHAT_MESSAGES : has
    USERS ||--o{ CHAT_MESSAGES : sends

    USERS ||--o{ WISHLISTS : saves
    BOOKS ||--o{ WISHLISTS : appears_in

    USERS ||--o{ NOTIFICATIONS : receives

    PURCHASE_REQUESTS ||--o| SALE_RECORDS : closes_as_sale
    BOOKS ||--o{ SALE_RECORDS : sold_as
    USERS ||--o{ SALE_RECORDS : owner_in
    USERS ||--o{ SALE_RECORDS : buyer_in

    SALE_RECORDS ||--o{ PAYMENTS : settled_by
    USERS ||--o{ PAYMENTS : payer
    USERS ||--o{ PAYMENTS : payee
```

## Notes

- The Payment model is introduced as a dedicated entity so each sale can track one or many payment events (full payment, retries, partial payments, or refunds).
- `PAYMENTS.status` can be values like `PENDING`, `SUCCESS`, `FAILED`, `REFUNDED`.
- Keep the existing `SALE_RECORDS.payment_mode` as a quick summary, or remove it later if `PAYMENTS.method` becomes the single source of truth.
- `WISHLISTS` should enforce a unique composite key on `(user_id, book_id)` (already reflected in your JPA entity).
