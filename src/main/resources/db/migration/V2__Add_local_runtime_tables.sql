ALTER TABLE test_cases ADD COLUMN IF NOT EXISTS priority VARCHAR(50);
ALTER TABLE test_cases ADD COLUMN IF NOT EXISTS requirement_id VARCHAR(255);
ALTER TABLE test_cases ADD COLUMN IF NOT EXISTS app_name VARCHAR(255);
ALTER TABLE test_cases ADD COLUMN IF NOT EXISTS module VARCHAR(255);
ALTER TABLE test_cases ADD COLUMN IF NOT EXISTS estimated_complexity DOUBLE PRECISION;
ALTER TABLE test_cases ADD COLUMN IF NOT EXISTS test_data_hints TEXT;

CREATE TABLE IF NOT EXISTS attachments (
    id UUID PRIMARY KEY,
    chat_message_id UUID NOT NULL REFERENCES chat_messages(id) ON DELETE CASCADE,
    original_file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    file_type VARCHAR(50) NOT NULL,
    category VARCHAR(50) NOT NULL,
    extracted_text TEXT,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_attachments_chat_message_id ON attachments(chat_message_id);

CREATE TABLE IF NOT EXISTS uploaded_files (
    id VARCHAR(255) PRIMARY KEY,
    original_filename VARCHAR(255),
    file_type VARCHAR(255),
    file_size_bytes BIGINT NOT NULL DEFAULT 0,
    extracted_text TEXT,
    language VARCHAR(255),
    confidence_score DOUBLE PRECISION,
    page_count INTEGER,
    status VARCHAR(50),
    error_message TEXT,
    uploaded_at TIMESTAMP,
    processed_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS code_artifacts (
    id VARCHAR(255) PRIMARY KEY,
    scenario_id VARCHAR(255),
    framework VARCHAR(50),
    language VARCHAR(50),
    code TEXT,
    confidence DOUBLE PRECISION,
    requires_human_review BOOLEAN,
    notes TEXT,
    generated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS code_artifact_files (
    artifact_id VARCHAR(255) NOT NULL REFERENCES code_artifacts(id) ON DELETE CASCADE,
    file_path VARCHAR(255),
    content TEXT
);

CREATE INDEX IF NOT EXISTS idx_code_artifact_files_artifact_id ON code_artifact_files(artifact_id);

CREATE TABLE IF NOT EXISTS code_artifact_dependencies (
    artifact_id VARCHAR(255) NOT NULL REFERENCES code_artifacts(id) ON DELETE CASCADE,
    dependencies VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_code_artifact_dependencies_artifact_id ON code_artifact_dependencies(artifact_id);

CREATE TABLE IF NOT EXISTS test_case_steps (
    test_case_id UUID NOT NULL REFERENCES test_cases(id) ON DELETE CASCADE,
    step_number INTEGER NOT NULL,
    action TEXT,
    test_data VARCHAR(255),
    expected_result TEXT
);

CREATE INDEX IF NOT EXISTS idx_test_case_steps_test_case_id ON test_case_steps(test_case_id);

CREATE TABLE IF NOT EXISTS test_case_expected_results (
    test_case_id UUID NOT NULL REFERENCES test_cases(id) ON DELETE CASCADE,
    expected_results VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_test_case_expected_results_test_case_id ON test_case_expected_results(test_case_id);

CREATE TABLE IF NOT EXISTS test_case_tags (
    test_case_id UUID NOT NULL REFERENCES test_cases(id) ON DELETE CASCADE,
    tags VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_test_case_tags_test_case_id ON test_case_tags(test_case_id);
