package com.jeferson.gestaoacoes.service;

import com.jeferson.gestaoacoes.dto.AutenticacaoResponseDTO;
import com.jeferson.gestaoacoes.dto.CadastroRequestDTO;
import com.jeferson.gestaoacoes.dto.LoginRequestDTO;
import com.jeferson.gestaoacoes.dto.UsuarioAutenticadoDTO;
import com.jeferson.gestaoacoes.exception.CredenciaisInvalidasException;
import com.jeferson.gestaoacoes.exception.EmailJaCadastradoException;
import com.jeferson.gestaoacoes.model.PerfilUsuario;
import com.jeferson.gestaoacoes.model.Usuario;
import com.jeferson.gestaoacoes.repository.UsuarioRepository;
import com.jeferson.gestaoacoes.security.JwtService;
import com.jeferson.gestaoacoes.security.UsuarioPrincipal;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AutenticacaoService {

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AutenticacaoService(UsuarioRepository repository, PasswordEncoder passwordEncoder,
                               AuthenticationManager authenticationManager, JwtService jwtService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AutenticacaoResponseDTO cadastrar(CadastroRequestDTO dto) {
        String email = normalizarEmail(dto.email());
        if (repository.existsByEmail(email)) {
            throw new EmailJaCadastradoException();
        }

        Usuario usuario = new Usuario();
        usuario.setNome(dto.nome().trim());
        usuario.setEmail(email);
        usuario.setSenhaHash(passwordEncoder.encode(dto.senha()));
        usuario.setPerfil(PerfilUsuario.USER);
        usuario.setAtivo(true);

        try {
            Usuario salvo = repository.saveAndFlush(usuario);
            return criarResposta(UsuarioPrincipal.from(salvo), salvo);
        } catch (DataIntegrityViolationException ex) {
            throw new EmailJaCadastradoException();
        }
    }

    @Transactional(readOnly = true)
    public AutenticacaoResponseDTO autenticar(LoginRequestDTO dto) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizarEmail(dto.email()), dto.senha()));
            UsuarioPrincipal principal = (UsuarioPrincipal) authentication.getPrincipal();
            Usuario usuario = repository.findById(principal.id()).orElseThrow(CredenciaisInvalidasException::new);
            return criarResposta(principal, usuario);
        } catch (AuthenticationException ex) {
            throw new CredenciaisInvalidasException();
        }
    }

    @Transactional(readOnly = true)
    public UsuarioAutenticadoDTO usuarioAutenticado(Long id) {
        return repository.findById(id)
                .map(this::toUsuarioAutenticado)
                .orElseThrow(CredenciaisInvalidasException::new);
    }

    private AutenticacaoResponseDTO criarResposta(UsuarioPrincipal principal, Usuario usuario) {
        return new AutenticacaoResponseDTO(
                jwtService.gerarToken(principal),
                "Bearer",
                jwtService.getExpiracaoSegundos(),
                toUsuarioAutenticado(usuario));
    }

    private UsuarioAutenticadoDTO toUsuarioAutenticado(Usuario usuario) {
        return new UsuarioAutenticadoDTO(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getPerfil(),
                usuario.getDataHoraCadastro());
    }

    static String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
