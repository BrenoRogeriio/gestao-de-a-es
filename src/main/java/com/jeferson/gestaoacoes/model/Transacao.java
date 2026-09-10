package com.jeferson.gestaoacoes.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "transacoes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Transacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "acao_id", nullable = false)
    private Acao acao;

    @ManyToOne(optional = false)
    @JoinColumn(name = "corretora_id", nullable = false)
    private Corretora corretora;

    // Nullable no banco apenas para preservar registros anteriores à autenticação.
    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_transacao", nullable = false, length = 10)
    private TipoTransacao tipoTransacao;

    @Column(nullable = false)
    private Integer quantidade;

    @Column(name = "valor_unitario", nullable = false, precision = 19, scale = 4)
    private BigDecimal valorUnitario;

    @Column(name = "preco_medio_operacao", precision = 19, scale = 4)
    private BigDecimal precoMedioOperacao;

    @Column(name = "resultado_realizado", precision = 29, scale = 4)
    private BigDecimal resultadoRealizado;

    @Column(name = "data_hora_transacao", nullable = false)
    private OffsetDateTime dataHoraTransacao;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;
}
