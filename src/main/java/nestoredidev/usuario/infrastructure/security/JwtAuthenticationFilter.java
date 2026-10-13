package nestoredidev.usuario.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import nestoredidev.usuario.infrastructure.adapter.output.persistence.UsuarioRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/** RN-USU-03: un usuario inactivo (o borrado) deja de ser válido aunque su token no haya vencido. */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            jwtService.parse(header.substring(7)).ifPresent(claims -> {
                Long id = Long.valueOf(claims.getSubject());
                usuarioRepository.findById(id)
                        .filter(u -> Boolean.TRUE.equals(u.getActivo()))
                        .ifPresent(u -> {
                            AuthUser principal = new AuthUser(u.getId(), u.getUsername(), u.getRol());
                            var auth = new UsernamePasswordAuthenticationToken(principal, null,
                                    List.of(new SimpleGrantedAuthority("ROLE_" + u.getRol().name())));
                            SecurityContextHolder.getContext().setAuthentication(auth);
                        });
            });
        }
        chain.doFilter(request, response);
    }
}
