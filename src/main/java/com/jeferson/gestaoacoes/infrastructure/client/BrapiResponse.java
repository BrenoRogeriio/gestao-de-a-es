package com.jeferson.gestaoacoes.infrastructure.client;

import java.util.List;

public record BrapiResponse(
        List<BrapiResult> results
) {}