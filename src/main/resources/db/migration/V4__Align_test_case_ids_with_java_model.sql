ALTER TABLE test_case_embeddings DROP CONSTRAINT IF EXISTS test_case_embeddings_test_case_id_fkey;
ALTER TABLE embedding_duplicates DROP CONSTRAINT IF EXISTS embedding_duplicates_test_case_id_1_fkey;
ALTER TABLE embedding_duplicates DROP CONSTRAINT IF EXISTS embedding_duplicates_test_case_id_2_fkey;
ALTER TABLE embedding_duplicates DROP CONSTRAINT IF EXISTS embedding_duplicates_merged_into_id_fkey;
ALTER TABLE test_case_steps DROP CONSTRAINT IF EXISTS test_case_steps_test_case_id_fkey;
ALTER TABLE test_case_expected_results DROP CONSTRAINT IF EXISTS test_case_expected_results_test_case_id_fkey;
ALTER TABLE test_case_tags DROP CONSTRAINT IF EXISTS test_case_tags_test_case_id_fkey;

ALTER TABLE test_cases
    ALTER COLUMN id TYPE VARCHAR(255) USING id::TEXT;

ALTER TABLE test_case_embeddings
    ALTER COLUMN test_case_id TYPE VARCHAR(255) USING test_case_id::TEXT;

ALTER TABLE embedding_duplicates
    ALTER COLUMN test_case_id_1 TYPE VARCHAR(255) USING test_case_id_1::TEXT,
    ALTER COLUMN test_case_id_2 TYPE VARCHAR(255) USING test_case_id_2::TEXT,
    ALTER COLUMN merged_into_id TYPE VARCHAR(255) USING merged_into_id::TEXT;

ALTER TABLE test_case_steps
    ALTER COLUMN test_case_id TYPE VARCHAR(255) USING test_case_id::TEXT;

ALTER TABLE test_case_expected_results
    ALTER COLUMN test_case_id TYPE VARCHAR(255) USING test_case_id::TEXT;

ALTER TABLE test_case_tags
    ALTER COLUMN test_case_id TYPE VARCHAR(255) USING test_case_id::TEXT;

ALTER TABLE test_case_embeddings
    ADD CONSTRAINT test_case_embeddings_test_case_id_fkey
    FOREIGN KEY (test_case_id) REFERENCES test_cases(id) ON DELETE CASCADE;

ALTER TABLE embedding_duplicates
    ADD CONSTRAINT embedding_duplicates_test_case_id_1_fkey
    FOREIGN KEY (test_case_id_1) REFERENCES test_cases(id) ON DELETE CASCADE;

ALTER TABLE embedding_duplicates
    ADD CONSTRAINT embedding_duplicates_test_case_id_2_fkey
    FOREIGN KEY (test_case_id_2) REFERENCES test_cases(id) ON DELETE CASCADE;

ALTER TABLE embedding_duplicates
    ADD CONSTRAINT embedding_duplicates_merged_into_id_fkey
    FOREIGN KEY (merged_into_id) REFERENCES test_cases(id) ON DELETE SET NULL;

ALTER TABLE test_case_steps
    ADD CONSTRAINT test_case_steps_test_case_id_fkey
    FOREIGN KEY (test_case_id) REFERENCES test_cases(id) ON DELETE CASCADE;

ALTER TABLE test_case_expected_results
    ADD CONSTRAINT test_case_expected_results_test_case_id_fkey
    FOREIGN KEY (test_case_id) REFERENCES test_cases(id) ON DELETE CASCADE;

ALTER TABLE test_case_tags
    ADD CONSTRAINT test_case_tags_test_case_id_fkey
    FOREIGN KEY (test_case_id) REFERENCES test_cases(id) ON DELETE CASCADE;
