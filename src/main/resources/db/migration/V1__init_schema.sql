-- V1__init_schema.sql
-- Core schema for the book lending microservice.

CREATE TABLE books (
    id               UUID PRIMARY KEY,
    title            VARCHAR(255) NOT NULL,
    author           VARCHAR(255) NOT NULL,
    isbn             VARCHAR(32)  NOT NULL UNIQUE,
    total_copies     INT          NOT NULL CHECK (total_copies >= 0),
    available_copies INT          NOT NULL CHECK (available_copies >= 0
                                                 AND available_copies <= total_copies)
);

CREATE TABLE members (
    id         UUID        PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    email      VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE loans (
    id          UUID        PRIMARY KEY,
    book_id     UUID        NOT NULL REFERENCES books(id),
    member_id   UUID        NOT NULL REFERENCES members(id),
    borrowed_at TIMESTAMPTZ NOT NULL,
    due_date    DATE        NOT NULL,
    returned_at TIMESTAMPTZ,
    CONSTRAINT loans_returned_after_borrowed
        CHECK (returned_at IS NULL OR returned_at >= borrowed_at)
);

-- Active/overdue loan lookups are the hot path; partial indexes keep them small.
CREATE INDEX idx_loans_member_active  ON loans (member_id)            WHERE returned_at IS NULL;
CREATE INDEX idx_loans_book_active    ON loans (book_id)              WHERE returned_at IS NULL;
CREATE INDEX idx_loans_member_overdue ON loans (member_id, due_date)  WHERE returned_at IS NULL;
CREATE INDEX idx_books_isbn           ON books (isbn);
