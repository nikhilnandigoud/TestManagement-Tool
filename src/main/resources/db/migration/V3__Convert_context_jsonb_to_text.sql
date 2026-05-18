ALTER TABLE conversation_context
    ALTER COLUMN active_guides TYPE TEXT USING active_guides::TEXT,
    ALTER COLUMN uploaded_files TYPE TEXT USING uploaded_files::TEXT,
    ALTER COLUMN embedding_stats TYPE TEXT USING embedding_stats::TEXT;
