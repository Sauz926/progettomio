package it.unicas.spring.springai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChatbotMessageEditingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VectorStore vectorStore;

    @MockBean
    private ChatClient.Builder chatClientBuilder;

    private ChatClient chatClient;

    @BeforeEach
    void setUp() {
        chatClient = mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);
        when(chatClientBuilder.build()).thenReturn(chatClient);
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void modifica_messaggio_inoltra_nuova_domanda_e_restituisce_risposta_rigenerata() throws Exception {
        Document chunk = new Document(
                "Per usare il macchinario servono guanti antitaglio e protezioni uditive certificate.",
                Map.of("fileName", "manuale-dpi.pdf", "page", 9)
        );
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(chunk));

        ArgumentCaptor<String> userPromptCaptor = ArgumentCaptor.forClass(String.class);
        when(chatClient.prompt()
                .system(anyString())
                .user(userPromptCaptor.capture())
                .call()
                .content())
                .thenReturn("{\"answer\":\"Risposta originale\",\"chunkIds\":[1]}")
                .thenReturn("{\"answer\":\"Risposta aggiornata dopo modifica\",\"chunkIds\":[1]}");

        String originalQuestion = "DOMANDA_ORIGINALE_TEST";
        mockMvc.perform(post("/api/chatbot/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "question": "DOMANDA_ORIGINALE_TEST"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value(equalTo("Risposta originale")));

        Map<String, Object> editedRequest = new LinkedHashMap<>();
        editedRequest.put("question", "DOMANDA_MODIFICATA_TEST");
        editedRequest.put("history", List.of(
                Map.of("role", "assistant", "content", "Ciao, chiedimi pure."),
                Map.of("role", "user", "content", "Quali DPI richiede la macchina?")
        ));

        mockMvc.perform(post("/api/chatbot/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(editedRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value(equalTo("Risposta aggiornata dopo modifica")))
                .andExpect(jsonPath("$.sources[0].fileName").value("manuale-dpi.pdf"))
                .andExpect(jsonPath("$.sources[0].reference").value("manuale-dpi.pdf, Pag. 9"));

        List<String> prompts = userPromptCaptor.getAllValues();
        assertThat(prompts.size(), equalTo(2));

        String editedPrompt = prompts.get(1);
        assertThat(editedPrompt, containsString("DOMANDA_MODIFICATA_TEST"));
        assertThat(editedPrompt, containsString("Utente: Quali DPI richiede la macchina?"));
        assertThat(editedPrompt, not(containsString(originalQuestion)));
    }
}
