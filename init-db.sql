-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Create users table
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

-- Create user_roles table (for @ElementCollection)
CREATE TABLE IF NOT EXISTS user_roles (
                                          user_id BIGINT NOT NULL,
                                          role VARCHAR(50) NOT NULL,
                                          CONSTRAINT fk_user_roles_users FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create documents table
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

-- Create macchinari table
CREATE TABLE IF NOT EXISTS macchinari (
                                          id BIGSERIAL PRIMARY KEY,
                                          nome VARCHAR(255) NOT NULL,
                                          descrizione TEXT,
                                          produttore VARCHAR(255),
                                          modello VARCHAR(255),
                                          numero_serie VARCHAR(255),
                                          anno_produzione INTEGER,
                                          categoria VARCHAR(255),
                                          specifiche_tecniche TEXT,
                                          data_creazione TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                          data_ultima_modifica TIMESTAMP
);

-- Create assessment_results table
CREATE TABLE IF NOT EXISTS assessment_results (
                                                  id BIGSERIAL PRIMARY KEY,
                                                  macchinario_id BIGINT NOT NULL,
                                                  risultato TEXT NOT NULL,
                                                  riepilogo_conformita TEXT,
                                                  non_conformita_rilevate TEXT,
                                                  raccomandazioni TEXT,
                                                  livello_rischio VARCHAR(50),
                                                  punteggio_conformita INTEGER,
                                                  documenti_utilizzati TEXT,
                                                  data_assessment TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                                  versione VARCHAR(50),
                                                  CONSTRAINT fk_assessment_macchinario FOREIGN KEY (macchinario_id) REFERENCES macchinari(id) ON DELETE CASCADE
);

GRANT ALL PRIVILEGES ON DATABASE ragassessment TO postgres;