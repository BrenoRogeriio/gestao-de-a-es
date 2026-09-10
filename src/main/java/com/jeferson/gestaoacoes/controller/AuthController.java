package com.jeferson.gestaoacoes.controller;

import com.jeferson.gestaoacoes.dto.AutenticacaoResponseDTO;
import com.jeferson.gestaoacoes.dto.CadastroRequestDTO;
import com.jeferson.gestaoacoes.dto.LoginRequestDTO;
import com.jeferson.gestaoacoes.dto.UsuarioAutenticadoDTO;
import com.jeferson.gestaoacoes.security.UsuarioPrincipal;
import com.jeferson.gestaoacoes.service.AutenticacaoService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AutenticacaoService service;

    public AuthController(AutenticacaoService service) {
        this.service = service;
    }

    @PostMapping("/register")
    @SecurityRequirements
    public ResponseEntity<AutenticacaoResponseDTO> cadastrar(@Valid @RequestBody CadastroRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.cadastrar(dto));
    }

    @PostMapping("/login")
    @SecurityRequirements
    public ResponseEntity<AutenticacaoResponseDTO> autenticar(@Valid @RequestBody LoginRequestDTO dto) {
        return ResponseEntity.ok(service.autenticar(dto));
    }

    @GetMapping("/me")
    public ResponseEntity<UsuarioAutenticadoDTO> usuarioAutenticado(
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(service.usuarioAutenticado(principal.id()));
    }
}
