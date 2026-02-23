package it.unicas.spring.springai;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PdfUploadMachineExtractionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VectorStore vectorStore;

    @MockBean
    private ChatClient.Builder chatClientBuilder;

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void pdf_upload_allows_machine_data_extraction_via_manual_endpoint() throws Exception {
        ChatClient chatClient = mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);
        when(chatClientBuilder.build()).thenReturn(chatClient);

        ArgumentCaptor<String> userPromptCaptor = ArgumentCaptor.forClass(String.class);

        String aiResponseJson = """
                {
                  "nome": null,
                  "produttore": "ACME Machinery",
                  "modello": "X-500",
                  "numeroSerie": "SN-TEST-001",
                  "annoProduzione": 2024,
                  "categoria": "Pressa",
                  "descrizione": "Pressa per test",
                  "specificheTecniche": "Potenza 5kW"
                }
                """;

        when(chatClient.prompt()
                .system(anyString())
                .user(userPromptCaptor.capture())
                .call()
                .content()).thenReturn(aiResponseJson);

        byte[] pdfBytes = createPdf("""
                Manuale Macchinario
                Produttore: ACME Machinery
                Modello: X-500
                Numero di serie: SN-TEST-001
                Anno di produzione: 2024
                Categoria: Pressa idraulica
                Specifiche tecniche: Potenza 5kW
                """);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "pressa_idraulica_test.pdf",
                "application/octet-stream",
                pdfBytes
        );

        mockMvc.perform(multipart("/api/macchinari/manual/extract")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.nome").value("pressa idraulica test"))
                .andExpect(jsonPath("$.produttore").value("ACME Machinery"))
                .andExpect(jsonPath("$.modello").value("X-500"))
                .andExpect(jsonPath("$.numeroSerie").value("SN-TEST-001"))
                .andExpect(jsonPath("$.annoProduzione").value(2024))
                .andExpect(jsonPath("$.categoria").value("Presse"))
                .andExpect(jsonPath("$.specificheTecniche").value("Potenza 5kW"));

        String userPrompt = userPromptCaptor.getValue();
        assertThat(userPrompt, containsString("Nome file: pressa_idraulica_test.pdf"));
        assertThat(userPrompt, containsString("Produttore"));
        assertThat(userPrompt, containsString("ACME"));
        assertThat(userPrompt, containsString("X-500"));
        assertThat(userPrompt, containsString("SN-TEST-001"));
    }

    private static byte[] createPdf(String content) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.newLineAtOffset(48, 760);

                String[] lines = (content == null ? "" : content).split("\\R");
                for (String line : lines) {
                    String safeLine = line == null ? "" : line;
                    if (!safeLine.isEmpty()) {
                        stream.showText(safeLine);
                    }
                    stream.newLineAtOffset(0, -16);
                }

                stream.endText();
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }
}

