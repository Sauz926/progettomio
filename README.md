# RAG Assessment Macchinari

Sistema di valutazione conformità macchinari basato su Spring AI e Regolo.ai con RAG (Retrieval-Augmented Generation).

## Funzionalità

- **Caricamento PDF**: Upload di regolamenti e documentazione tecnica
- **RAG (Retrieval-Augmented Generation)**: Ricerca semantica sui documenti caricati
- **Assessment Macchinari**: Valutazione automatica di conformità basata sul Regolamento Macchine
- **Interfaccia Web Moderna**: Dashboard intuitiva per gestione documenti e macchinari
- **Autenticazione**: Sistema di login con Spring Security

## Prerequisiti

- Java 17+
- Maven 3.8+
- Docker & Docker Compose (per PostgreSQL + PGVector)
- Account Regolo.ai con API Key

## Configurazione

### 1. Avvia il database PostgreSQL con PGVector

```bash
docker-compose up -d
```

Questo avvierà PostgreSQL 16 con l'estensione pgvector sulla porta 5432.

### 2. Configura l'API Key di Regolo.ai

Imposta la variabile d'ambiente `REGOLO_API_KEY`:

**Windows PowerShell:**
```powershell
$env:REGOLO_API_KEY="your-api-key-here"
```

**Linux/Mac:**
```bash
export REGOLO_API_KEY="your-api-key-here"
```

Oppure modifica direttamente `application.properties`:
```properties
spring.ai.openai.api-key=your-api-key-here
```

### 3. Avvia l'applicazione

```bash
./mvnw spring-boot:run
```

Oppure su Windows:
```powershell
.\mvnw.cmd spring-boot:run
```

## Accesso

L'applicazione sarà disponibile su: **http://localhost:8080**

### Credenziali di default

| Ruolo | Username | Password |
|-------|----------|----------|
| Admin | admin | admin123 |
| User | user | user123 |

## Architettura

```
┌─────────────────────────────────────────────────────────────┐
│                    Frontend (HTML/CSS/JS)                   │
├─────────────────────────────────────────────────────────────┤
│                  Spring Boot Controllers                     │
│  ┌─────────────┐  ┌──────────────┐  ┌────────────────────┐ │
│  │ Document    │  │ Macchinario  │  │ Assessment         │ │
│  │ Controller  │  │ Controller   │  │ Controller         │ │
│  └─────────────┘  └──────────────┘  └────────────────────┘ │
├─────────────────────────────────────────────────────────────┤
│                       Services                               │
│  ┌─────────────────┐  ┌─────────────┐  ┌────────────────┐  │
│  │ PdfIngestion    │  │ RAG         │  │ Assessment     │  │
│  │ Service         │  │ Service     │  │ Service        │  │
│  └─────────────────┘  └─────────────┘  └────────────────┘  │
├─────────────────────────────────────────────────────────────┤
│                    Spring AI                                 │
│  ┌─────────────────────────┐  ┌──────────────────────────┐ │
│  │ PDF Document Reader     │  │ Chat Client (Regolo.ai)  │ │
│  └─────────────────────────┘  └──────────────────────────┘ │
├─────────────────────────────────────────────────────────────┤
│                    Data Layer                                │
│  ┌─────────────────────────┐  ┌──────────────────────────┐ │
│  │ PostgreSQL (JPA)        │  │ PGVector (Embeddings)    │ │
│  └─────────────────────────┘  └──────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

## API Endpoints

### Documenti
- `POST /api/documents/upload` - Upload PDF
- `GET /api/documents` - Lista documenti
- `GET /api/documents/{id}` - Dettaglio documento
- `GET /api/documents/{id}/download` - Download PDF
- `DELETE /api/documents/{id}` - Elimina documento

### Macchinari
- `POST /api/macchinari` - Crea macchinario
- `GET /api/macchinari` - Lista macchinari
- `GET /api/macchinari/{id}` - Dettaglio macchinario
- `PUT /api/macchinari/{id}` - Aggiorna macchinario
- `DELETE /api/macchinari/{id}` - Elimina macchinario
- `POST /api/macchinari/{id}/assessment` - Genera assessment
- `GET /api/macchinari/{id}/assessments` - Storico assessment

### Assessment
- `GET /api/assessments` - Lista tutti gli assessment
- `GET /api/assessments/{id}` - Dettaglio assessment

## Uso

1. **Carica i PDF dei regolamenti** nella sezione "Documenti"
   - Il sistema processerà automaticamente i PDF e creerà gli embeddings

2. **Aggiungi un macchinario** nella sezione "Macchinari"
   - Inserisci tutte le informazioni disponibili per un assessment più accurato

3. **Genera l'assessment** cliccando su "📊 Assessment"
   - Il sistema utilizzerà i documenti caricati per valutare la conformità
   - L'assessment includerà: riepilogo, non conformità, raccomandazioni, livello di rischio

## Tecnologie

- **Backend**: Spring Boot 3.4, Spring AI, Spring Security, Spring Data JPA
- **Database**: PostgreSQL 16 + PGVector
- **LLM**: Regolo.ai (Llama-3.3-70B-Instruct)
- **Frontend**: HTML5, CSS3, JavaScript vanilla
- **Containerization**: Docker, Docker Compose

## License

MIT License
