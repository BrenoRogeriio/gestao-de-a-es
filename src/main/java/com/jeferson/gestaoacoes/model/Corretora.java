package com.jeferson.gestaoacoes.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "corretoras")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Corretora {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, length = 14, unique = true)
    private String cnpj;

    @Column(name = "razao_social", nullable = false)
    private String razaoSocial;

    @Column(name = "nome_fantasia")
    private String nomeFantasia;

    private String email;

    @Column(length = 50)
    private String telefone;

    @Column(nullable = false, length = 8)
    private String cep;

    @Column(nullable = false)
    private String logradouro;

    @Column(length = 50)
    private String numero;

    private String complemento;

    @Column(nullable = false)
    private String bairro;

    @Column(nullable = false)
    private String cidade;

    @Column(nullable = false, length = 2)
    private String uf;

    @Column(name = "situacao_cadastral", nullable = false, length = 50)
    private String situacaoCadastral;

    @Column(name = "status_cvm", nullable = false, length = 50)
    private String statusCvm;

    @Column(name = "data_hora_cadastro", nullable = false)
    private OffsetDateTime dataHoraCadastro;
}