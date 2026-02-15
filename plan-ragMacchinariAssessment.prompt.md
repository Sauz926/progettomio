## Plan: Servizio RAG per Assessment Macchinari con Spring AI

Creazione di un'applicazione Spring Boot con Spring AI e Regolo.ai per caricare PDF di regolamenti, effettuare RAG (Retrieval-Augmented Generation) e generare assessment sui macchinari. Include database MySQL/PostgreSQL per persistenza, vector store per embeddings e frontend moderno.

### Steps

1. **Aggiornare [pom.xml](C:\Users\mmoli\Desktop\AA2526\DP&N\spring\springai\pom.xml)** — Aggiungere dipendenze: `spring-ai-starter`, `spring-ai-pdf-document-reader`, `spring-ai-pgvector-store` (o ChromaDB), `spring-boot-starter-data-jpa`, driver PostgreSQL/MySQL, `spring-boot-starter-web`, e Thymeleaf per l'interfaccia.

2. **Creare entità JPA e repository** — Definire `Document` (id, nome, contenuto, data caricamento), `Macchinario` (id, nome, descrizione, metadata), `AssessmentResult` (id, macchinario, risultato, data) in `it.unicas.spring.springai.model` con relativi repository.

3. **Implementare servizio PDF e RAG** — Creare `PdfIngestionService` per leggere PDF, generare embeddings con Spring AI e salvarli nel vector store. Creare `RagService` per eseguire query semantiche sui documenti caricati.

4. **Implementare servizio Assessment** — Creare `AssessmentService` che utilizza `RagService` per recuperare contesto rilevante dai regolamenti e genera assessment tramite Regolo.ai/LLM configurato.

5. **Creare REST controller** — Implementare `PdfController` (upload PDF, lista documenti) e `MacchinarioController` (CRUD macchinari, richiesta assessment) con endpoint `/api/documents` e `/api/macchinari`.

6. **Sviluppare interfaccia web** — Creare pagine HTML/CSS/JS in `src/main/resources/static/` con due sezioni: upload PDF (drag & drop, lista documenti) e form macchinari con visualizzazione assessment. Stile moderno con CSS custom o framework leggero.

7. **Configurare database e vector store** — Configurare datasource in `application.properties` per PostgreSQL. Configurare PGVector per gli embeddings.

8. **Provider LLM per Regolo.ai** — Usare un modello da regolo.ai come Llama-3.3-70B-Instruct.

9. **Autenticazione** — È necessaria l'autenticazione utente per proteggere l'accesso all'applicazione. Aggiungere Spring Security



