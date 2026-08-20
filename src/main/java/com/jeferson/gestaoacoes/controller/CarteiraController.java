package com.jeferson.gestaoacoes.controller;

import com.jeferson.gestaoacoes.dto.HistoricoResponseDTO;
import com.jeferson.gestaoacoes.dto.PosicaoResponseDTO;
import com.jeferson.gestaoacoes.dto.TransacaoRequestDTO;
import com.jeferson.gestaoacoes.service.CarteiraService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/carteira")
@CrossOrigin(origins = "*")
public class CarteiraController {

    private final CarteiraService carteiraService;

    public CarteiraController(CarteiraService carteiraService) {
        this.carteiraService = carteiraService;
    }

    @PostMapping("/comprar")
    public ResponseEntity<Void> comprar(@Valid @RequestBody TransacaoRequestDTO dto) {
        carteiraService.registrarCompra(dto);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/vender")
    public ResponseEntity<Void> vender(@Valid @RequestBody TransacaoRequestDTO dto) {
        carteiraService.registrarVenda(dto);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/posicao")
    public ResponseEntity<List<PosicaoResponseDTO>> verPosicao() {
        return ResponseEntity.ok(carteiraService.listarPosicoes());
    }

    @GetMapping("/historico")
    public ResponseEntity<List<HistoricoResponseDTO>> listarHistorico() {
        return ResponseEntity.ok(carteiraService.listarHistorico());
    }
}