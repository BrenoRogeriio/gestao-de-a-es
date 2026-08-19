package com.jeferson.gestaoacoes.service;

import com.jeferson.gestaoacoes.dto.AcaoRequestDTO;
import com.jeferson.gestaoacoes.exception.RegraNegocioException;
import com.jeferson.gestaoacoes.infrastructure.client.BrapiClient;
import com.jeferson.gestaoacoes.infrastructure.client.TwelveDataClient;
import com.jeferson.gestaoacoes.mapper.AcaoMapper;
import com.jeferson.gestaoacoes.model.Mercado;
import com.jeferson.gestaoacoes.repository.AcaoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AcaoServiceTest {

    @Mock
    private AcaoRepository repository;

    @Mock
    private AcaoMapper mapper;

    @Mock
    private BrapiClient brapiClient;

    @Mock
    private TwelveDataClient twelveDataClient;

    @InjectMocks
    private AcaoService acaoService;

    @Test
    void deveLancarExcecaoQuandoAcaoJaExistir() {
        // GIVEN (Dado que...)
        AcaoRequestDTO dto = new AcaoRequestDTO("PETR4", Mercado.BRASIL);

        // Simulamos que o banco de dados vai responder "true" para a verificação de existência
        when(repository.existsByTickerAndMercado("PETR4", Mercado.BRASIL)).thenReturn(true);

        // WHEN & THEN (Quando tentarmos cadastrar, Então deve dar erro)
        RegraNegocioException exception = assertThrows(RegraNegocioException.class, () -> {
            acaoService.cadastrar(dto);
        });

        // Verificamos se a mensagem de erro é exatamente a que definimos na Regra de Negócio
        assertEquals("Ação já cadastrada para este mercado.", exception.getMessage());

        // Garantimos que nenhuma API externa foi chamada por acidente
        verifyNoInteractions(brapiClient, twelveDataClient);
    }
}