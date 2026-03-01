package it.unicas.spring.springai;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChatbotCsvExportContractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VectorStore vectorStore;

    @MockBean
    private ChatClient.Builder chatClientBuilder;

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void pagina_index_espone_controlli_csv_e_feedback_per_modifica_messaggi() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"chatbotCsvModal\"")))
                .andExpect(content().string(containsString("id=\"chatbotCsvYesBtn\"")))
                .andExpect(content().string(containsString("id=\"chatbotCsvNoBtn\"")))
                .andExpect(content().string(containsString("id=\"chatbotCsvCancelBtn\"")))
                .andExpect(content().string(containsString("id=\"chatbotExportStatus\"")))
                .andExpect(content().string(containsString("id=\"chatbotEditStatus\"")))
                .andExpect(content().string(containsString("Salvare la chat in CSV prima del riavvio?")));
    }

    @Test
    void javascript_export_csv_prevede_snapshot_header_temporali_e_download_file() throws Exception {
        mockMvc.perform(get("/js/app.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("function buildChatbotConversationSnapshot()")))
                .andExpect(content().string(containsString("function downloadChatbotConversationCsv(snapshot)")))
                .andExpect(content().string(containsString("function buildChatbotConversationCsv(snapshot)")))
                .andExpect(content().string(containsString("['indice', 'ruolo', 'messaggio', 'data_ora_messaggio', 'data_ora_inizio_conversazione', 'data_ora_fine_conversazione', 'data_ora_export_csv']")))
                .andExpect(content().string(containsString("const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });")))
                .andExpect(content().string(containsString("window.URL.createObjectURL(blob);")))
                .andExpect(content().string(containsString("a.download = filename;")))
                .andExpect(content().string(containsString("replaceAll('\\n', '\\\\n');")))
                .andExpect(content().string(containsString("formatCsvDateTime(msg.ts)")));
    }

    @Test
    void javascript_flusso_combinato_restart_csv_resetta_stato_e_gestisce_errori() throws Exception {
        mockMvc.perform(get("/js/app.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("function handleChatbotCsvRestartChoice(saveCsv)")))
                .andExpect(content().string(containsString("const snapshot = saveCsv ? buildChatbotConversationSnapshot() : null;")))
                .andExpect(content().string(containsString("restartChatbotConversationUi();")))
                .andExpect(content().string(containsString("const downloaded = downloadChatbotConversationCsv(snapshot);")))
                .andExpect(content().string(containsString("CSV scaricato correttamente (${count} messaggi esportati).")))
                .andExpect(content().string(containsString("Errore durante il download del CSV. Riprova.")))
                .andExpect(content().string(containsString("chatbotMessageEditingState = null;")))
                .andExpect(content().string(containsString("clearChatbotEditStatus();")));
    }

    @Test
    void css_prevede_layout_responsive_per_csv_modal_e_modifica_messaggi() throws Exception {
        mockMvc.perform(get("/css/style.css"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(".chatbot-csv-modal-content {")))
                .andExpect(content().string(containsString(".chatbot-csv-actions {")))
                .andExpect(content().string(containsString(".chat-message-edit-input {")))
                .andExpect(content().string(containsString("@media (max-width: 768px)")))
                .andExpect(content().string(containsString(".chatbot-csv-actions .btn {")))
                .andExpect(content().string(containsString(".chatbot-shell {")))
                .andExpect(content().string(containsString("min-height: calc(100dvh - 260px);")));
    }
}
