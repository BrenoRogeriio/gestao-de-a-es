package com.jeferson.gestaoacoes.security;

import com.jeferson.gestaoacoes.model.PerfilUsuario;
import com.jeferson.gestaoacoes.model.Usuario;
import com.jeferson.gestaoacoes.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioAtualServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deveObterUsuarioExclusivamenteDoPrincipalAutenticado() {
        Usuario usuario = usuario(42L);
        UsuarioPrincipal principal = UsuarioPrincipal.from(usuario);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        when(usuarioRepository.getReferenceById(42L)).thenReturn(usuario);

        UsuarioAtualService service = new UsuarioAtualService(usuarioRepository);

        assertEquals(42L, service.obterId());
        assertSame(usuario, service.obterReferencia());
    }

    @Test
    void deveRejeitarPrincipalQueNaoVeioDaAutenticacaoJwt() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("usuario-forjado", null, java.util.List.of()));

        UsuarioAtualService service = new UsuarioAtualService(usuarioRepository);

        assertThrows(AuthenticationCredentialsNotFoundException.class, service::obterId);
    }

    private Usuario usuario(Long id) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNome("Usuario");
        usuario.setEmail("usuario@example.com");
        usuario.setSenhaHash("hash");
        usuario.setPerfil(PerfilUsuario.USER);
        usuario.setAtivo(true);
        return usuario;
    }
}
