-- The webhook controller currently derives repository_id from the GitHub
-- fullName via UUID.nameUUIDFromBytes(...), but doesn't yet upsert a row
-- into a repositories table to satisfy the parent reference. That makes the
-- FK fire on every real webhook (latent since V1 — only surfaced once we
-- actually delivered a real pull_request event in Day 7).
--
-- Drop the constraint so PRs can be queued. A future phase will add a real
-- Repository entity with upsert-on-webhook semantics and reinstate this FK.
ALTER TABLE review_jobs DROP CONSTRAINT IF EXISTS review_jobs_repository_id_fkey;
