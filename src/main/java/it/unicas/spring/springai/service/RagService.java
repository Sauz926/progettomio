package it.unicas.spring.springai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagService {

    private final VectorStore vectorStore;

    private static final int DEFAULT_TOP_K = 5;
    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.7;

    /**
     * Esegue una ricerca vettoriale con i parametri di default.
     * Chiamata dai servizi di dominio che non devono personalizzare il numero di chunk.
     *
     * @param query testo della domanda/requisito da cercare
     * @return lista di documenti rilevanti recuperati dal vector store
     */
    public List<Document> searchRelevantDocuments(String query) {
        return searchRelevantDocuments(query, DEFAULT_TOP_K);
    }

    /**
     * Esegue la ricerca vettoriale con numero risultati configurabile.
     * Chiamata da chatbot, consultazione guidata e altri servizi che controllano la profondità RAG.
     *
     * @param query testo su cui fare similarità semantica
     * @param topK numero massimo di chunk da recuperare
     * @return documenti più simili alla query
     */
    public List<Document> searchRelevantDocuments(String query, int topK) {
        log.info("Searching for relevant documents with query: {}", query);

        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(DEFAULT_SIMILARITY_THRESHOLD)
                .build();

        List<Document> results = vectorStore.similaritySearch(searchRequest);

        log.info("Found {} relevant documents", results.size());
        return results;
    }

    /**
     * Costruisce direttamente il contesto testuale partendo dalla query.
     * Chiamata dai servizi che hanno bisogno di un contesto testuale sintetico per il prompt.
     *
     * @param query domanda/descrizione da usare per la ricerca
     * @return contesto pronto da passare al modello LLM
     */
    public String buildContext(String query) {
        List<Document> relevantDocs = searchRelevantDocuments(query);
        return buildContextFromDocuments(relevantDocs);
    }

    /**
     * Concatena i chunk recuperati in un testo unico leggibile dal prompt.
     * Chiamata internamente da {@link #buildContext(String)} e dai servizi RAG applicativi.
     *
     * @param documents chunk recuperati dal vector store
     * @return stringa contestuale con metadati fonte e contenuto
     */
    public String buildContextFromDocuments(List<Document> documents) {
        if (documents.isEmpty()) {
            return "Nessun documento di riferimento trovato nel database.";
        }

        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("=== DOCUMENTI DI RIFERIMENTO ===\n\n");

        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            contextBuilder.append(String.format("--- Documento %d ---\n", i + 1));

            if (doc.getMetadata().containsKey("fileName")) {
                contextBuilder.append("Fonte: ").append(doc.getMetadata().get("fileName")).append("\n");
            }
            if (doc.getMetadata().containsKey("page")) {
                contextBuilder.append("Pagina: ").append(doc.getMetadata().get("page")).append("\n");
            }

            contextBuilder.append("Contenuto:\n").append(doc.getText()).append("\n\n");
        }

        return contextBuilder.toString();
    }

    /**
     * Estrae e deduplica i nomi delle fonti dai metadati dei chunk.
     * Chiamata principalmente dai servizi che devono esporre le fonti usate in output.
     *
     * @param documents chunk usati nell'elaborazione
     * @return lista nomi documento senza duplicati
     */
    public List<String> getDocumentSources(List<Document> documents) {
        return documents.stream()
                .map(doc -> {
                    log.info("Document metadata: {}", doc.getMetadata());  // Debug
                    Map<String, Object> metadata = doc.getMetadata();
                    if (metadata.containsKey("fileName")) {
                        return metadata.get("fileName").toString();
                    } else if (metadata.containsKey("file_name")) {
                        return metadata.get("file_name").toString();
                    } else if (metadata.containsKey("filename")) {
                        return metadata.get("filename").toString();
                    } else if (metadata.containsKey("source")) {
                        return metadata.get("source").toString();
                    }
                    return "Documento sconosciuto";
                })
                .distinct()
                .toList();
    }
}
