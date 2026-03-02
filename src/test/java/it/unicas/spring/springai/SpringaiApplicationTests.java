package it.unicas.spring.springai;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class SpringaiApplicationTests {

    /**
     * Verifica il bootstrap del contesto Spring senza eccezioni.
     * Chiamata dal runner JUnit 5 come smoke test di avvio applicazione.
     */
    @Test
    void contextLoads() {
        // Test will pass if context loads successfully
    }

}
