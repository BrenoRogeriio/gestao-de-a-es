package com.jeferson.gestaoacoes.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Trata as nossas regras de negócio customizadas (Ex: Ticker já existe, CEP inválido)
    @ExceptionHandler(RegraNegocioException.class)
    public ProblemDetail handleRegraNegocioException(RegraNegocioException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problemDetail.setTitle("Violação de Regra de Negócio");
        problemDetail.setType(URI.create("https://gestao-acoes.com/erros/regra-de-negocio"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    // Trata os erros de validação do Bean Validation (@NotBlank, @Pattern, etc nos DTOs)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationExceptions(MethodArgumentNotValidException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Um ou mais campos estão inválidos.");
        problemDetail.setTitle("Erro de Validação");
        problemDetail.setType(URI.create("https://gestao-acoes.com/erros/validacao-invalida"));
        problemDetail.setProperty("timestamp", Instant.now());

        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        // Adiciona a lista de campos que falharam na resposta
        problemDetail.setProperty("erros", fieldErrors);

        return problemDetail;
    }
}