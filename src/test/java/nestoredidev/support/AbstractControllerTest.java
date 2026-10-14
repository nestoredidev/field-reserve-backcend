package nestoredidev.support;

import io.jsonwebtoken.Jwts;
import nestoredidev.common.domain.Rol;
import nestoredidev.common.exception.GlobalExceptionHandler;
import nestoredidev.usuario.infrastructure.adapter.output.persistence.UsuarioEntity;
import nestoredidev.usuario.infrastructure.adapter.output.persistence.UsuarioRepository;
import nestoredidev.usuario.infrastructure.security.JwtAuthenticationFilter;
import nestoredidev.usuario.infrastructure.security.JwtService;
import nestoredidev.usuario.infrastructure.security.SecurityConfig;
import nestoredidev.usuario.infrastructure.security.SecurityCurrentUserAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Optional;

import static org.mockito.Mockito.when;

/**
 * Base de los tests de controller: {@code @WebMvcTest} con la cadena de seguridad REAL (SecurityConfig +
 * JwtAuthenticationFilter + SecurityCurrentUserAdapter) y el manejador global de errores. Solo se mockean el
 * servicio del controller (en cada test), el parseo del JWT y la búsqueda del usuario. Los tests anotan su
 * clase con {@code @WebMvcTest(XController.class)}.
 */
@Import({WebSecurityTestConfig.class, SecurityConfig.class, JwtAuthenticationFilter.class, SecurityCurrentUserAdapter.class,
        GlobalExceptionHandler.class})
public abstract class AbstractControllerTest {

    public static final Long ADMIN_ID = 1L;
    public static final Long ENCARGADO_ID = 2L;
    protected static final String ADMIN = "Bearer admin-token";
    protected static final String ENCARGADO = "Bearer encargado-token";

    @Autowired
    protected MockMvc mvc;
    @MockitoBean
    protected JwtService jwtService;
    @MockitoBean
    protected UsuarioRepository usuarioRepository;

    @BeforeEach
    void autenticacion() {
        registrar("admin-token", ADMIN_ID, "admin", Rol.ADMIN, true);
        registrar("encargado-token", ENCARGADO_ID, "encargado", Rol.ENCARGADO, true);
        registrar("inactivo-token", 3L, "inactivo", Rol.ADMIN, false);
    }

    private void registrar(String token, Long id, String username, Rol rol, boolean activo) {
        when(jwtService.parse(token)).thenReturn(Optional.of(Jwts.claims().subject(String.valueOf(id)).build()));
        UsuarioEntity u = new UsuarioEntity();
        u.setId(id);
        u.setUsername(username);
        u.setRol(rol);
        u.setActivo(activo);
        when(usuarioRepository.findById(id)).thenReturn(Optional.of(u));
    }

    protected static MockHttpServletRequestBuilder comoAdmin(MockHttpServletRequestBuilder request) {
        return request.header("Authorization", ADMIN);
    }

    protected static MockHttpServletRequestBuilder comoEncargado(MockHttpServletRequestBuilder request) {
        return request.header("Authorization", ENCARGADO);
    }
}
