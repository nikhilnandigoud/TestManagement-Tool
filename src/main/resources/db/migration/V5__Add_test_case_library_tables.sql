-- Migration V5: Add test case library tables
-- Adds support for saving test cases to personal library with metadata

CREATE TABLE test_case_library (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    test_case_id VARCHAR(36) NOT NULL,
    library_title VARCHAR(255) NOT NULL,
    library_description TEXT,
    category VARCHAR(50) NOT NULL DEFAULT 'POSITIVE',
    priority VARCHAR(50) NOT NULL DEFAULT 'MEDIUM',
    quality_score VARCHAR(10),
    is_public BOOLEAN DEFAULT FALSE,
    created_by VARCHAR(255),
    notes TEXT,
    usage_count INTEGER DEFAULT 0,
    saved_to_library_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (test_case_id) REFERENCES test_cases(id) ON DELETE CASCADE
);

CREATE INDEX idx_test_case_library_test_case_id ON test_case_library(test_case_id);
CREATE INDEX idx_test_case_library_category ON test_case_library(category);
CREATE INDEX idx_test_case_library_priority ON test_case_library(priority);
CREATE INDEX idx_test_case_library_created_by ON test_case_library(created_by);
CREATE INDEX idx_test_case_library_is_public ON test_case_library(is_public);
CREATE INDEX idx_test_case_library_usage_count ON test_case_library(usage_count);
CREATE INDEX idx_test_case_library_saved_at ON test_case_library(saved_to_library_at);

-- Collection table for tags in the library
CREATE TABLE library_tags (
    library_id VARCHAR(36) NOT NULL REFERENCES test_case_library(id) ON DELETE CASCADE,
    tags VARCHAR(255) NOT NULL,
    PRIMARY KEY (library_id, tags)
);

CREATE INDEX idx_library_tags_tags ON library_tags(tags);

-- Comment for documentation
COMMENT ON TABLE test_case_library IS 'Stores test cases saved to personal library with metadata for reuse and organization';
COMMENT ON COLUMN test_case_library.library_title IS 'Custom title for the library entry';
COMMENT ON COLUMN test_case_library.category IS 'Test case category: POSITIVE, NEGATIVE, EDGE_CASE, BOUNDARY, SECURITY, PERFORMANCE';
COMMENT ON COLUMN test_case_library.quality_score IS 'Quality assessment score as percentage string (e.g., "82%")';
COMMENT ON COLUMN test_case_library.usage_count IS 'Tracks how many times this test case has been reused';
