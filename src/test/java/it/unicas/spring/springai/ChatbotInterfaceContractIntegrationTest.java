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
class ChatbotInterfaceContractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VectorStore vectorStore;

    @MockBean
    private ChatClient.Builder chatClientBuilder;

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void pagina_index_contiene_tutti_i_componenti_chat_essenziali() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"chatbot\"")))
                .andExpect(content().string(containsString("id=\"chatbotMessages\"")))
                .andExpect(content().string(containsString("id=\"chatbotComposer\"")))
                .andExpect(content().string(containsString("id=\"chatbotInput\"")))
                .andExpect(content().string(containsString("id=\"chatbotSendBtn\"")))
                .andExpect(content().string(containsString("id=\"chatbotNewChatBtn\"")))
                .andExpect(content().string(containsString("aria-live=\"polite\"")));
    }

    @Test
    void javascript_chatbot_gestisce_render_e_flusso_invio_in_modo_affidabile() throws Exception {
        mockMvc.perform(get("/js/app.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("function submitChatbotChat()")))
                .andExpect(content().string(containsString("chatbotInput.disabled = true;")))
                .andExpect(content().string(containsString("chatbotInput.disabled = false;")))
                .andExpect(content().string(containsString("if (chatbotSendBtn) chatbotSendBtn.disabled = true;")))
                .andExpect(content().string(containsString("if (chatbotSendBtn) chatbotSendBtn.disabled = false;")))
                .andExpect(content().string(containsString("chatbotMessages.scrollTop = chatbotMessages.scrollHeight;")))
                .andExpect(content().string(containsString("escapeHtml(text).replaceAll('\\n', '<br>')")))
                .andExpect(content().string(containsString("renderChatbotMessageSourcesHtml")));
    }

    @Test
    void css_chatbot_ha_regole_layout_chiare_e_responsive() throws Exception {
        mockMvc.perform(get("/css/style.css"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(".chatbot-shell {")))
                .andExpect(content().string(containsString(".chatbot-messages {")))
                .andExpect(content().string(containsString(".chatbot-composer {")))
                .andExpect(content().string(containsString(".chatbot-shell .chat-message--assistant .chat-bubble {")))
                .andExpect(content().string(containsString("@media (max-width: 768px)")))
                .andExpect(content().string(containsString("min-height: calc(100dvh - 260px);")));
    }
}
