package it.unicas.spring.springai;

import it.unicas.spring.springai.model.AssessmentResult;
import it.unicas.spring.springai.model.Macchinario;
import it.unicas.spring.springai.repository.AssessmentResultRepository;
import it.unicas.spring.springai.repository.MacchinarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AssessmentChatContextIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MacchinarioRepository macchinarioRepository;

    @Autowired
    private AssessmentResultRepository assessmentResultRepository;

    @MockBean
    private VectorStore vectorStore;

    @MockBean
    private ChatClient.Builder chatClientBuilder;

    private ChatClient chatClient;

    @BeforeEach
    void setUp() {
        assessmentResultRepository.deleteAll();
        macchinarioRepository.deleteAll();

        chatClient = mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);
        when(chatClientBuilder.build()).thenReturn(chatClient);
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void assessment_chat_contestualizza_solo_sulla_macchina_selezionata() throws Exception {
        AssessmentResult assessmentA = salvaAssessment(
                "MACCHINA_A_TOKEN",
                "DIFETTO_A_TOKEN",
                "RACCOMANDAZIONE_A_TOKEN"
        );
        salvaAssessment(
                "MACCHINA_B_TOKEN",
                "DIFETTO_B_TOKEN",
                "RACCOMANDAZIONE_B_TOKEN"
        );

        ArgumentCaptor<String> userPromptCaptor = ArgumentCaptor.forClass(String.class);
        when(chatClient.prompt()
                .system(anyString())
                .user(userPromptCaptor.capture())
                .call()
                .content()).thenReturn("Risposta contestualizzata sulla macchina corrente.");

        mockMvc.perform(post("/api/assessments/{id}/chat", assessmentA.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "question": "Quali sono le priorita immediate?"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value(containsString("macchina corrente")));

        String prompt = userPromptCaptor.getValue();
        assertThat(prompt, containsString("MACCHINA_A_TOKEN"));
        assertThat(prompt, containsString("DIFETTO_A_TOKEN"));
        assertThat(prompt, not(containsString("MACCHINA_B_TOKEN")));
        assertThat(prompt, not(containsString("DIFETTO_B_TOKEN")));
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void assessment_chat_invia_difetti_e_raccomandazioni_per_risposte_pertinenti() throws Exception {
        AssessmentResult assessmentA = salvaAssessment(
                "MACCHINA_PERTINENZA_TOKEN",
                "DIFETTO_PERTINENTE_RIPARO_INTERBLOCCATO",
                "RACCOMANDAZIONE_PERTINENTE_INSTALLARE_RIPARO"
        );

        ArgumentCaptor<String> userPromptCaptor = ArgumentCaptor.forClass(String.class);
        when(chatClient.prompt()
                .system(anyString())
                .user(userPromptCaptor.capture())
                .call()
                .content()).thenReturn("""
                Azioni pratiche:
                1) Installare subito il riparo interbloccato.
                2) Verificare con check operativo il ripristino della protezione.
                """);

        mockMvc.perform(post("/api/assessments/{id}/chat", assessmentA.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "question": "Che azioni devo fare sui difetti emersi?"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value(containsString("Installare subito il riparo interbloccato")))
                .andExpect(jsonPath("$.answer").value(containsString("Verificare con check operativo")));

        String prompt = userPromptCaptor.getValue();
        assertThat(prompt, containsString("=== NON CONFORMIT"));
        assertThat(prompt, containsString("=== RACCOMANDAZIONI"));
        assertThat(prompt, containsString("DIFETTO_PERTINENTE_RIPARO_INTERBLOCCATO"));
        assertThat(prompt, containsString("RACCOMANDAZIONE_PERTINENTE_INSTALLARE_RIPARO"));
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void assessment_chat_isola_i_dati_tra_assessment_di_macchine_diverse() throws Exception {
        AssessmentResult assessmentA = salvaAssessment(
                "MACCHINA_ISOLAMENTO_A_TOKEN",
                "DIFETTO_ISOLAMENTO_A_TOKEN",
                "RACCOMANDAZIONE_ISOLAMENTO_A_TOKEN"
        );
        AssessmentResult assessmentB = salvaAssessment(
                "MACCHINA_ISOLAMENTO_B_TOKEN",
                "DIFETTO_ISOLAMENTO_B_TOKEN",
                "RACCOMANDAZIONE_ISOLAMENTO_B_TOKEN"
        );

        ArgumentCaptor<String> userPromptCaptor = ArgumentCaptor.forClass(String.class);
        when(chatClient.prompt()
                .system(anyString())
                .user(userPromptCaptor.capture())
                .call()
                .content()).thenReturn(
                        "Risposta assessment A",
                        "Risposta assessment B"
                );

        mockMvc.perform(post("/api/assessments/{id}/chat", assessmentA.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "question": "Dammi priorita per assessment A"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Risposta assessment A"));

        mockMvc.perform(post("/api/assessments/{id}/chat", assessmentB.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "question": "Dammi priorita per assessment B"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Risposta assessment B"));

        List<String> prompts = userPromptCaptor.getAllValues();
        assertThat(prompts.size(), equalTo(2));

        String promptA = prompts.get(0);
        assertThat(promptA, containsString("MACCHINA_ISOLAMENTO_A_TOKEN"));
        assertThat(promptA, containsString("DIFETTO_ISOLAMENTO_A_TOKEN"));
        assertThat(promptA, not(containsString("MACCHINA_ISOLAMENTO_B_TOKEN")));
        assertThat(promptA, not(containsString("DIFETTO_ISOLAMENTO_B_TOKEN")));

        String promptB = prompts.get(1);
        assertThat(promptB, containsString("MACCHINA_ISOLAMENTO_B_TOKEN"));
        assertThat(promptB, containsString("DIFETTO_ISOLAMENTO_B_TOKEN"));
        assertThat(promptB, not(containsString("MACCHINA_ISOLAMENTO_A_TOKEN")));
        assertThat(promptB, not(containsString("DIFETTO_ISOLAMENTO_A_TOKEN")));
    }

    private AssessmentResult salvaAssessment(String machineToken, String difettoToken, String raccomandazioneToken) {
        Macchinario macchinario = new Macchinario();
        macchinario.setNome("Nome " + machineToken);
        macchinario.setDescrizione("Descrizione " + machineToken);
        macchinario.setProduttore("Produttore " + machineToken);
        macchinario.setModello("Modello " + machineToken);
        macchinario.setNumeroSerie("Serie " + machineToken);
        macchinario.setAnnoProduzione(2024);
        macchinario.setCategoria("Categoria " + machineToken);
        macchinario.setSpecificheTecniche("Specifiche " + machineToken);

        Macchinario savedMacchinario = macchinarioRepository.save(macchinario);

        AssessmentResult assessment = new AssessmentResult();
        assessment.setMacchinario(savedMacchinario);
        assessment.setRisultato("Risultato " + machineToken);
        assessment.setRiepilogoConformita("Riepilogo " + machineToken);
        assessment.setNonConformitaRilevate("""
                - %s
                - ALTRA_NC_%s
                """.formatted(difettoToken, machineToken));
        assessment.setRaccomandazioni("""
                - %s
                - ALTRA_RACCOMANDAZIONE_%s
                """.formatted(raccomandazioneToken, machineToken));
        assessment.setLivelloRischio("ALTO");
        assessment.setPunteggioConformita(45);
        assessment.setDocumentiUtilizzati("manuale-" + machineToken + ".pdf");
        assessment.setDataAssessment(LocalDateTime.now());
        assessment.setVersione("1.0");

        return assessmentResultRepository.save(assessment);
    }
}
