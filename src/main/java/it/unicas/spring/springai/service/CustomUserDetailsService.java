package it.unicas.spring.springai.service;

import it.unicas.spring.springai.model.User;
import it.unicas.spring.springai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Carica l'utente applicativo per autenticazione Spring Security.
     * Chiamata automaticamente da {@link org.springframework.security.authentication.AuthenticationManager}
     * durante il login; traduce ruoli applicativi nel formato {@code ROLE_*}.
     *
     * @param username username inserito in fase di login
     * @return dettagli utente compatibili con Spring Security
     * @throws UsernameNotFoundException se l'utente non esiste nel repository
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utente non trovato: " + username));

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.isEnabled(),
                true, true, true,
                user.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .collect(Collectors.toList())
        );
    }
}
