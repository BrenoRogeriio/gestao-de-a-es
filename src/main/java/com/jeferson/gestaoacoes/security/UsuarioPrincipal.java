package com.jeferson.gestaoacoes.security;

import com.jeferson.gestaoacoes.model.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public final class UsuarioPrincipal implements UserDetails {

    private final Long id;
    private final String nome;
    private final String email;
    private final String password;
    private final String perfil;
    private final boolean ativo;

    private UsuarioPrincipal(Long id, String nome, String email, String password,
                             String perfil, boolean ativo) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.password = password;
        this.perfil = perfil;
        this.ativo = ativo;
    }

    public static UsuarioPrincipal from(Usuario usuario) {
        return new UsuarioPrincipal(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getSenhaHash(),
                usuario.getPerfil().name(),
                usuario.isAtivo());
    }

    public Long id() {
        return id;
    }

    public String nome() {
        return nome;
    }

    public String email() {
        return email;
    }

    public String perfil() {
        return perfil;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + perfil));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return ativo;
    }

    @Override
    public String toString() {
        return "UsuarioPrincipal{id=%s, email='%s', perfil='%s', ativo=%s}"
                .formatted(id, email, perfil, ativo);
    }
}
