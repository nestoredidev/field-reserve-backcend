package nestoredidev.usuario.application.ports.input.dto;

public record LoginResponse(String token, String tokenType, long expiresInSeconds, UsuarioDto usuario) {
}
