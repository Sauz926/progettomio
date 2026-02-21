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
     * Cerca documenti rilevanti nel vector store basandosi sulla query
     */
    public List<Document> searchRelevantDocuments(String query) {
        return searchRelevantDocuments(query, DEFAULT_TOP_K);
    }

    /**
     * Cerca documenti rilevanti con numero specifico di risultati
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
     * Crea un contesto concatenato dai documenti rilevanti
     */
    public String buildContext(String query) {
        List<Document> relevantDocs = searchRelevantDocuments(query);
        return buildContextFromDocuments(relevantDocs);
    }

    /**
     * Crea un contesto da una lista di documenti
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
     * Ottiene i nomi dei documenti utilizzati nel contesto
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
