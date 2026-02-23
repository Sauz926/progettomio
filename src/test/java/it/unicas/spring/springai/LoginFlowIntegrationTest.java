package it.unicas.spring.springai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LoginFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VectorStore vectorStore;

    @MockBean
    private ChatClient.Builder chatClientBuilder;

    @Test
    void user_can_login_and_reach_final_results_flow() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Credenziali di test")));

        MvcResult login = mockMvc.perform(SecurityMockMvcRequestBuilders.formLogin("/login")
                        .user("user")
                        .password("user123"))
                .andExpect(authenticated().withUsername("user"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/index.html"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);

        Document ragChunk = new Document(
                "Articolo 10 comma 2: requisiti essenziali di sicurezza e marcatura CE.",
                Map.of("fileName", "Regolamento UE 2023-1230.pdf", "page", 10)
        );

        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(ragChunk));

        String explainableAssessmentJson = """
                {
                  "riepilogoConformita": "Riepilogo conformità (test).",
                  "nonConformitaRilevate": [
                    {"text": "Mancanza marcatura CE visibile", "chunkIds": [1]}
                  ],
                  "raccomandazioni": [
                    {"text": "Applicare marcatura CE e aggiornare la documentazione", "chunkIds": [1]}
                  ],
                  "livelloRischio": "MEDIO",
                  "punteggioConformita": 72
                }
                """;

        ChatClient chatClient = mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);
        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()
                .system(anyString())
                .user(anyString())
                .call()
                .content()).thenReturn(explainableAssessmentJson);

        mockMvc.perform(get("/index.html").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("RAG Assessment")));

        String machinePayload = """
                {
                  "nome": "Pressa Idraulica Test",
                  "produttore": "ACME",
                  "modello": "X-500",
                  "numeroSerie": "SN-TEST-001",
                  "annoProduzione": 2024,
                  "categoria": "Presse",
                  "descrizione": "Macchinario usato per test end-to-end",
                  "specificheTecniche": "Potenza 5kW"
                }
                """;

        MvcResult createdMachineResult = mockMvc.perform(post("/api/macchinari")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(machinePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nome").value("Pressa Idraulica Test"))
                .andReturn();

        JsonNode createdMachine = objectMapper.readTree(createdMachineResult.getResponse().getContentAsString());
        long machineId = createdMachine.get("id").asLong();

        MvcResult createdAssessmentResult = mockMvc.perform(post("/api/macchinari/{id}/assessment", machineId).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.macchinarioId").value(machineId))
                .andExpect(jsonPath("$.macchinarioNome").value("Pressa Idraulica Test"))
                .andExpect(jsonPath("$.punteggioConformita").value(72))
                .andExpect(jsonPath("$.livelloRischio").value("MEDIO"))
                .andExpect(jsonPath("$.documentiUtilizzati").value(containsString("Regolamento UE 2023-1230.pdf")))
                .andReturn();

        JsonNode createdAssessment = objectMapper.readTree(createdAssessmentResult.getResponse().getContentAsString());
        long assessmentId = createdAssessment.get("id").asLong();

        mockMvc.perform(get("/api/macchinari/{id}/assessments", machineId).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(assessmentId))
                .andExpect(jsonPath("$[0].macchinarioId").value(machineId));

        mockMvc.perform(get("/api/assessments").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(assessmentId))
                .andExpect(jsonPath("$[0].macchinarioNome").value("Pressa Idraulica Test"));

        mockMvc.perform(get("/api/assessments/{id}", assessmentId).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(assessmentId))
                .andExpect(jsonPath("$.macchinarioNome").value("Pressa Idraulica Test"))
                .andExpect(jsonPath("$.riepilogoConformita").value("Riepilogo conformità (test)."));

        MvcResult pdfDownload = mockMvc.perform(get("/api/macchinari/{id}/assessments/latest/pdf", machineId)
                        .session(session)
                        .accept(MediaType.APPLICATION_PDF))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("application/pdf")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment")))
                .andReturn();

        byte[] pdfBytes = pdfDownload.getResponse().getContentAsByteArray();
        org.hamcrest.MatcherAssert.assertThat(pdfBytes.length, greaterThan(100));
    }
}
