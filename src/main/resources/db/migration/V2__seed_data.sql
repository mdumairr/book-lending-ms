-- V2__seed_data.sql
-- Seeded members and an initial book catalog.

INSERT INTO members (id, name, email, created_at) VALUES
 (gen_random_uuid(), 'Demo Member', 'member@library.com', now()),
 (gen_random_uuid(), 'Demo Member 2', 'member2@library.com', now())
ON CONFLICT (email) DO NOTHING;

INSERT INTO books (id, title, author, isbn, total_copies, available_copies) VALUES
 (gen_random_uuid(), 'Clean Code', 'Robert C. Martin', '9780132350884', 3, 3),
 (gen_random_uuid(), 'The Pragmatic Programmer', 'Andrew Hunt, David Thomas', '9780135957059', 2, 2),
 (gen_random_uuid(), 'Design Patterns', 'Erich Gamma, Richard Helm, Ralph Johnson, John Vlissides', '9780201633610', 2, 2),
 (gen_random_uuid(), 'Refactoring', 'Martin Fowler', '9780134757599', 4, 4),
 (gen_random_uuid(), 'Domain-Driven Design', 'Eric Evans', '9780321125217', 1, 1),
 (gen_random_uuid(), 'Effective Java', 'Joshua Bloch', '9780134685991', 3, 3),
 (gen_random_uuid(), 'Java Concurrency in Practice', 'Brian Goetz', '9780321349606', 2, 2),
 (gen_random_uuid(), 'Spring in Action', 'Craig Walls', '9781617294945', 3, 3),
 (gen_random_uuid(), 'Head First Design Patterns', 'Eric Freeman, Elisabeth Robson', '9780596007126', 4, 4),
 (gen_random_uuid(), 'Code Complete', 'Steve McConnell', '9780735619678', 2, 2)
ON CONFLICT (isbn) DO NOTHING;
