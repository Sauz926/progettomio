package it.unicas.spring.springai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
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

import java.util.ArrayList;
import java.util.HashMap;
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
    void user_can_login_receive_recommendation_save_and_download_pdf() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().is3xxRedirection());

        MvcResult login = mockMvc.perform(SecurityMockMvcRequestBuilders.formLogin("/login")
                        .user("user")
                        .password("user123"))
                .andExpect(authenticated().withUsername("user"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/index.html"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);

        mockMvc.perform(get("/index.html").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Device Compass AI")));

        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(
                new Document("Pixel 9 offre Android pulito, buona fotocamera e batteria affidabile.", Map.of("fileName", "pixel9.pdf", "page", 4)),
                new Document("iPhone 16 Pro eccelle in video e integrazione ecosistema Apple.", Map.of("fileName", "iphone16pro.pdf", "page", 2))
        ));

        ChatClient chatClient = mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);
        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt().system(anyString()).user(anyString()).call().content()).thenReturn(
                """
                {
                  "fase":"RACCOMANDAZIONE",
                  "raccomandazionePrincipale":"Pixel 9",
                  "motivazione":"Pixel 9 e la scelta migliore per budget, fotografia e preferenza Android.",
                  "alternative":[
                    {
                      "nome":"Pixel 9",
                      "marca":"Google",
                      "prezzoEuro":899,
                      "motivazione":"Bilancia fotocamera, software pulito e prezzo coerente.",
                      "punteggio":92
                    },
                    {
                      "nome":"iPhone 16 Pro",
                      "marca":"Apple",
                      "prezzoEuro":1349,
                      "motivazione":"Alternativa premium per chi privilegia video ed ecosistema.",
                      "punteggio":80
                    }
                  ]
                }
                """,
                """
                {
                  "fase":"RACCOMANDAZIONE",
                  "raccomandazionePrincipale":"Pixel 9",
                  "motivazione":"Pixel 9 e la scelta migliore per budget, fotografia e preferenza Android.",
                  "alternative":[
                    {
                      "nome":"Pixel 9",
                      "marca":"Google",
                      "prezzoEuro":899,
                      "motivazione":"Bilancia fotocamera, software pulito e prezzo coerente.",
                      "punteggio":92
                    },
                    {
                      "nome":"iPhone 16 Pro",
                      "marca":"Apple",
                      "prezzoEuro":1349,
                      "motivazione":"Alternativa premium per chi privilegia video ed ecosistema.",
                      "punteggio":80
                    }
                  ]
                }
                """
        );

        String sessioneId = "session-test-001";
        List<Map<String, String>> history = new ArrayList<>();
        history.add(turn("assistant", "Ciao! Sono il tuo assistente per la scelta del dispositivo perfetto. Per iniziare, dimmi: stai cercando uno smartphone, uno smartwatch o un tablet?"));

        MvcResult firstResponse = mockMvc.perform(post("/api/consultazione/chat")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "sessioneId", sessioneId,
                                "userMessage", "Cerco uno smartphone",
                                "conversationHistory", history
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fase").value("RACCOLTA_DATI"))
                .andReturn();
        history.add(turn("user", "Cerco uno smartphone"));
        history.add(turn("assistant", objectMapper.readTree(firstResponse.getResponse().getContentAsString()).get("risposta").asText()));

        MvcResult secondResponse = mockMvc.perform(post("/api/consultazione/chat")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "sessioneId", sessioneId,
                                "userMessage", "Massimo 900 euro",
                                "conversationHistory", history
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fase").value("RACCOLTA_DATI"))
                .andReturn();
        history.add(turn("user", "Massimo 900 euro"));
        history.add(turn("assistant", objectMapper.readTree(secondResponse.getResponse().getContentAsString()).get("risposta").asText()));

        MvcResult thirdResponse = mockMvc.perform(post("/api/consultazione/chat")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "sessioneId", sessioneId,
                                "userMessage", "Lo uso soprattutto per foto e video",
                                "conversationHistory", history
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fase").value("ANALISI"))
                .andReturn();
        history.add(turn("user", "Lo uso soprattutto per foto e video"));
        history.add(turn("assistant", objectMapper.readTree(thirdResponse.getResponse().getContentAsString()).get("risposta").asText()));

        MvcResult finalResponse = mockMvc.perform(post("/api/consultazione/chat")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "sessioneId", sessioneId,
                                "userMessage", "Preferisco Android e per me conta molto la fotocamera",
                                "conversationHistory", history
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fase").value("RACCOMANDAZIONE"))
                .andExpect(jsonPath("$.dispositiviConsigliati[0].nome").value("Pixel 9"))
                .andReturn();
        history.add(turn("user", "Preferisco Android e per me conta molto la fotocamera"));
        history.add(turn("assistant", objectMapper.readTree(finalResponse.getResponse().getContentAsString()).get("risposta").asText()));

        mockMvc.perform(post("/api/consultazione/salva")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "sessioneId", sessioneId,
                                "conversationHistory", history
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.raccomandazionePrincipale").value("Pixel 9"));

        mockMvc.perform(get("/api/consultazione/storico").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sessioneId").value(sessioneId))
                .andExpect(jsonPath("$[0].raccomandazionePrincipale").value("Pixel 9"));

        MvcResult pdfDownload = mockMvc.perform(get("/api/consultazione/{sessioneId}/pdf", sessioneId)
                        .session(session)
                        .accept(MediaType.APPLICATION_PDF))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("application/pdf")))
                .andReturn();

        byte[] pdfBytes = pdfDownload.getResponse().getContentAsByteArray();
        org.hamcrest.MatcherAssert.assertThat(pdfBytes.length, greaterThan(100));
    }

    private Map<String, String> turn(String role, String content) {
        Map<String, String> turn = new HashMap<>();
        turn.put("role", role);
        turn.put("content", content);
        return turn;
    }
}
