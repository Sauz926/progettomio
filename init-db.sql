CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(500) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    nome VARCHAR(100) NOT NULL,
    cognome VARCHAR(100) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    data_creazione TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ultimo_accesso TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role VARCHAR(50) NOT NULL,
    CONSTRAINT fk_user_roles_users FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS documents (
    id BIGSERIAL PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    file_size BIGINT,
    content_type VARCHAR(100),
    file_content BYTEA NOT NULL,
    testo_estratto TEXT,
    upload_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed BOOLEAN DEFAULT FALSE,
    chunk_count INTEGER,
    description TEXT
);

CREATE TABLE IF NOT EXISTS dispositivi (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    marca VARCHAR(255) NOT NULL,
    modello VARCHAR(255),
    categoria VARCHAR(30) NOT NULL,
    prezzo_euro NUMERIC(10, 2),
    sistema_operativo VARCHAR(100),
    descrizione TEXT,
    specifiche_tecniche TEXT,
    punti_di_forza TEXT,
    punti_deboli TEXT,
    disponibile BOOLEAN NOT NULL DEFAULT TRUE,
    data_creazione TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_ultima_modifica TIMESTAMP
);

CREATE TABLE IF NOT EXISTS profili_utente (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    sessione_id VARCHAR(120) NOT NULL,
    esigenze_summary TEXT,
    budget_max INTEGER,
    categoria_interesse VARCHAR(30),
    raccomandazione_principale TEXT,
    motivazione TEXT,
    alternative TEXT,
    data_creazione TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_profili_utente_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_profili_utente_user_sessione UNIQUE (user_id, sessione_id)
);

CREATE INDEX IF NOT EXISTS idx_documents_upload_date ON documents(upload_date DESC);
CREATE INDEX IF NOT EXISTS idx_dispositivi_categoria ON dispositivi(categoria);
CREATE INDEX IF NOT EXISTS idx_dispositivi_prezzo ON dispositivi(prezzo_euro);
CREATE INDEX IF NOT EXISTS idx_profili_utente_user ON profili_utente(user_id, data_creazione DESC);

GRANT ALL PRIVILEGES ON DATABASE ragassessment TO postgres;
