-- Local PostgreSQL fallback migration.
-- This version does not require the pgvector server extension. Embeddings are
-- stored as PostgreSQL REAL[] arrays and similarity is computed in Java.

CREATE TABLE conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    scenario_text TEXT,
    user_id VARCHAR(255),
    status VARCHAR(50) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_conversations_user_id ON conversations(user_id);
CREATE INDEX idx_conversations_created_at ON conversations(created_at);

CREATE TABLE chat_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    message_type VARCHAR(50) DEFAULT 'CHAT',
    action_type VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_chat_messages_conversation_id ON chat_messages(conversation_id);
CREATE INDEX idx_chat_messages_role ON chat_messages(role);

CREATE TABLE test_cases (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    preconditions TEXT,
    steps JSONB,
    expected_results TEXT,
    actual_results TEXT,
    status VARCHAR(50) DEFAULT 'DRAFT',
    version INTEGER DEFAULT 1,
    created_by UUID REFERENCES conversations(id) ON DELETE SET NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_test_cases_conversation_id ON test_cases(conversation_id);
CREATE INDEX idx_test_cases_status ON test_cases(status);

CREATE TABLE chat_message_embeddings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chat_message_id UUID NOT NULL REFERENCES chat_messages(id) ON DELETE CASCADE,
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    embedding_model VARCHAR(100) DEFAULT 'text-embedding-3-small',
    embedding_dimensions INTEGER DEFAULT 1536,
    content_embedding REAL[],
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_chat_message_embeddings_chat_message_id ON chat_message_embeddings(chat_message_id);
CREATE INDEX idx_chat_message_embeddings_conversation_id ON chat_message_embeddings(conversation_id);

CREATE TABLE test_case_embeddings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    test_case_id UUID NOT NULL REFERENCES test_cases(id) ON DELETE CASCADE,
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    embedding_model VARCHAR(100) DEFAULT 'text-embedding-3-small',
    embedding_dimensions INTEGER DEFAULT 1536,
    title_embedding REAL[],
    steps_embedding REAL[],
    results_embedding REAL[],
    combined_embedding REAL[],
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_test_case_embeddings_test_case_id ON test_case_embeddings(test_case_id);
CREATE INDEX idx_test_case_embeddings_conversation_id ON test_case_embeddings(conversation_id);

CREATE TABLE embedding_duplicates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    test_case_id_1 UUID NOT NULL REFERENCES test_cases(id) ON DELETE CASCADE,
    test_case_id_2 UUID NOT NULL REFERENCES test_cases(id) ON DELETE CASCADE,
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    similarity_score FLOAT NOT NULL,
    detected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) DEFAULT 'PENDING',
    merged_into_id UUID REFERENCES test_cases(id) ON DELETE SET NULL
);

CREATE INDEX idx_embedding_duplicates_test_case_id_1 ON embedding_duplicates(test_case_id_1);
CREATE INDEX idx_embedding_duplicates_test_case_id_2 ON embedding_duplicates(test_case_id_2);
CREATE INDEX idx_embedding_duplicates_conversation_id ON embedding_duplicates(conversation_id);
CREATE INDEX idx_embedding_duplicates_similarity ON embedding_duplicates(similarity_score);

CREATE TABLE conversation_context (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL UNIQUE REFERENCES conversations(id) ON DELETE CASCADE,
    active_scenario TEXT,
    active_guides JSONB,
    uploaded_files JSONB,
    vector_context_hits INTEGER DEFAULT 0,
    last_query_time TIMESTAMP,
    embedding_stats JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_guides (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    content TEXT,
    category VARCHAR(100),
    version INTEGER DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(file_path)
);

CREATE OR REPLACE FUNCTION update_modified_column() RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_conversations_modified BEFORE UPDATE ON conversations
    FOR EACH ROW EXECUTE FUNCTION update_modified_column();

CREATE TRIGGER update_test_cases_modified BEFORE UPDATE ON test_cases
    FOR EACH ROW EXECUTE FUNCTION update_modified_column();

CREATE TRIGGER update_test_case_embeddings_modified BEFORE UPDATE ON test_case_embeddings
    FOR EACH ROW EXECUTE FUNCTION update_modified_column();

CREATE TRIGGER update_conversation_context_modified BEFORE UPDATE ON conversation_context
    FOR EACH ROW EXECUTE FUNCTION update_modified_column();

-- Compatibility placeholders. The application performs local similarity search
-- through VectorSearchService until pgvector is available on the PostgreSQL host.
CREATE OR REPLACE FUNCTION find_similar_test_cases(
    p_embedding REAL[],
    p_similarity_threshold FLOAT DEFAULT 0.7,
    p_limit INT DEFAULT 5
) RETURNS TABLE(
    test_case_id UUID,
    similarity_score FLOAT,
    title VARCHAR(255)
) AS $$
SELECT NULL::UUID, NULL::FLOAT, NULL::VARCHAR(255)
WHERE FALSE;
$$ LANGUAGE SQL;

CREATE OR REPLACE FUNCTION detect_duplicate_test_cases(
    p_conversation_id UUID,
    p_similarity_threshold FLOAT DEFAULT 0.85
) RETURNS TABLE(
    test_case_id_1 UUID,
    test_case_id_2 UUID,
    similarity_score FLOAT
) AS $$
SELECT NULL::UUID, NULL::UUID, NULL::FLOAT
WHERE FALSE;
$$ LANGUAGE SQL;
