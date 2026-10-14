package nestoredidev.support;

import nestoredidev.usuario.infrastructure.security.JwtAuthenticationFilter;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

/**
 * En el slice @WebMvcTest no se activa la seguridad web sola; esto habilita HttpSecurity para SecurityConfig.
 * También evita que MockMvc registre JwtAuthenticationFilter por fuera de la cadena de seguridad (ahí el
 * SecurityContextHolderFilter descartaría la autenticación que el filtro acaba de poner).
 */
@TestConfiguration(proxyBeanMethods = false)
@EnableWebSecurity
public class WebSecurityTestConfig {

    @Bean
    FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterSoloDentroDeLaCadena(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
