package it.unicas.spring.springai.config;

import it.unicas.spring.springai.model.CategoriaDispositivo;
import it.unicas.spring.springai.model.Dispositivo;
import it.unicas.spring.springai.model.User;
import it.unicas.spring.springai.repository.DispositivoRepository;
import it.unicas.spring.springai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;
import java.math.BigDecimal;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final UserRepository userRepository;
    private final DispositivoRepository dispositivoRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initData() {
        return args -> {
            // Crea utente admin di default se non esiste
            if (!userRepository.existsByUsername("admin")) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setEmail("admin@example.com");
                admin.setNome("Admin");
                admin.setCognome("Sistema");
                admin.setEnabled(true);
                admin.setRoles(Set.of("ADMIN", "USER"));

                userRepository.save(admin);
                log.info("Created default admin user (username: admin, password: admin123)");
            }

            // Crea utente test se non esiste
            if (!userRepository.existsByUsername("user")) {
                User user = new User();
                user.setUsername("user");
                user.setPassword(passwordEncoder.encode("user123"));
                user.setEmail("user@example.com");
                user.setNome("Utente");
                user.setCognome("Test");
                user.setEnabled(true);
                user.setRoles(Set.of("USER"));

                userRepository.save(user);
                log.info("Created default test user (username: user, password: user123)");
            }

            if (dispositivoRepository.count() == 0) {
                dispositivoRepository.save(createDevice(
                        "iPhone 16 Pro",
                        "Apple",
                        "A3293",
                        CategoriaDispositivo.SMARTPHONE,
                        new BigDecimal("1349.00"),
                        "iOS 18",
                        "Smartphone premium pensato per fotografia avanzata, video e integrazione con ecosistema Apple.",
                        "Display OLED ProMotion, chip Apple A18 Pro, fotocamera tripla, autonomia migliorata.",
                        "Fotocamera di alto livello, materiali premium, ottima fluidita",
                        "Prezzo elevato, ecosistema piu chiuso"
                ));
                dispositivoRepository.save(createDevice(
                        "Pixel 9",
                        "Google",
                        "GXQ96",
                        CategoriaDispositivo.SMARTPHONE,
                        new BigDecimal("899.00"),
                        "Android 15",
                        "Smartphone equilibrato per AI on-device, foto computazionale e uso quotidiano.",
                        "Display OLED 120Hz, Tensor G4, fotocamera principale avanzata, 5G.",
                        "Fotografia affidabile, Android pulito, ottimo software",
                        "Meno flessibile nel gaming pesante rispetto a concorrenti top"
                ));
                dispositivoRepository.save(createDevice(
                        "Apple Watch Series 10",
                        "Apple",
                        "42mm GPS",
                        CategoriaDispositivo.SMARTWATCH,
                        new BigDecimal("479.00"),
                        "watchOS 11",
                        "Smartwatch focalizzato su salute, notifiche e allenamenti per utenti iPhone.",
                        "Display ampio, ECG, rilevazione sonno, GPS, resistenza acqua.",
                        "Esperienza software curata, ottime funzioni salute, integrazione Apple",
                        "Richiede iPhone per il meglio, autonomia inferiore ai Garmin"
                ));
                dispositivoRepository.save(createDevice(
                        "Garmin Venu 3",
                        "Garmin",
                        "Venu 3",
                        CategoriaDispositivo.SMARTWATCH,
                        new BigDecimal("449.00"),
                        "Garmin OS",
                        "Smartwatch per fitness e benessere con focus su autonomia e metriche sportive.",
                        "GPS multi-band, monitoraggio sonno, metriche recovery, batteria multi-day.",
                        "Autonomia eccellente, dati sportivi avanzati, GPS accurato",
                        "Meno app e funzioni smart rispetto ai watch generalisti"
                ));
                dispositivoRepository.save(createDevice(
                        "iPad Air 13",
                        "Apple",
                        "M2",
                        CategoriaDispositivo.TABLET,
                        new BigDecimal("969.00"),
                        "iPadOS 18",
                        "Tablet leggero orientato a studio, creativita e produttivita mobile.",
                        "Chip M2, display 13 pollici, supporto Apple Pencil Pro e Magic Keyboard.",
                        "Prestazioni elevate, ottimo ecosistema app, supporto penna",
                        "Accessori costosi, file system meno flessibile di un laptop"
                ));
                dispositivoRepository.save(createDevice(
                        "Galaxy Tab S10+",
                        "Samsung",
                        "SM-X826",
                        CategoriaDispositivo.TABLET,
                        new BigDecimal("1099.00"),
                        "Android 15",
                        "Tablet premium per multitasking, entertainment e produttivita con pennino incluso.",
                        "Display AMOLED 120Hz, S Pen inclusa, multitasking avanzato, batteria capiente.",
                        "Display eccellente, ottimo per produttivita, penna inclusa",
                        "Prezzo alto, ottimizzazione app tablet non sempre uniforme"
                ));

                log.info("Created sample device catalog with 6 entries");
            }
        };
    }

    private Dispositivo createDevice(
            String nome,
            String marca,
            String modello,
            CategoriaDispositivo categoria,
            BigDecimal prezzoEuro,
            String sistemaOperativo,
            String descrizione,
            String specificheTecniche,
            String puntiDiForza,
            String puntiDeboli
    ) {
        Dispositivo dispositivo = new Dispositivo();
        dispositivo.setNome(nome);
        dispositivo.setMarca(marca);
        dispositivo.setModello(modello);
        dispositivo.setCategoria(categoria);
        dispositivo.setPrezzoEuro(prezzoEuro);
        dispositivo.setSistemaOperativo(sistemaOperativo);
        dispositivo.setDescrizione(descrizione);
        dispositivo.setSpecificheTecniche(specificheTecniche);
        dispositivo.setPuntiDiForza(puntiDiForza);
        dispositivo.setPuntiDeboli(puntiDeboli);
        dispositivo.setDisponibile(true);
        return dispositivo;
    }
}
