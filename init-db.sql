CREATE TABLE IF NOT EXISTS da_files (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT NOT NULL DEFAULT 1,
    filename      VARCHAR(255) NOT NULL,
    file_path     VARCHAR(500) NOT NULL,
    row_count     INT,
    col_count     INT,
    columns_json  TEXT,
    profile_json  TEXT,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS da_sessions (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_id     BIGINT,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (file_id) REFERENCES da_files(id)
);

CREATE TABLE IF NOT EXISTS da_qa_history (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id      BIGINT,
    question        TEXT NOT NULL,
    answer          TEXT,
    chart_json      TEXT,
    code_generated  TEXT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (session_id) REFERENCES da_sessions(id)
);
