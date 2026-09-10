# Arquitetura

## Visão geral

```text
Frontend React/Vite
        │ HTTP/JSON + Bearer JWT
        ▼
Backend Spring Boot
        ▼
Controller → Service → Repository → PostgreSQL
                    │
                    └→ clientes OpenFeign → APIs externas
```

O frontend concentra navegação, apresentação e estado da sessão. A API valida entradas nos DTOs, aplica autenticação e regras de negócio, persiste pelo Spring Data JPA e mantém o schema com Liquibase. Respostas de erro seguem RFC 9457/Problem Details.

## Componentes

- **Controllers:** expõem autenticação, ações, corretoras e carteira por HTTP.
- **Services:** aplicam validações, integrações e regras financeiras.
- **Repositories:** encapsulam persistência e consultas por JPA.
- **Security:** autenticação stateless, validação JWT, respostas JSON 401/403 e obtenção do usuário atual.
- **Clientes externos:** ViaCEP, BrasilAPI, Brapi e Twelve Data via OpenFeign.
- **Frontend:** React/Vite, rotas protegidas e sessão JWT em `sessionStorage`.

## Modelo de domínio e propriedade

| Entidade/dado | Escopo | Papel |
|---|---|---|
| `Usuario` | identidade | conta, perfil e estado de ativação |
| `Acao` | global | ativo de referência e cotação |
| `Corretora` | global | instituição de referência |
| `Posicao` | por usuário | quantidade e preço médio consolidados por ação |
| `Transacao` | por usuário | compra/venda e resultados da operação |
| Histórico e resumo | por usuário | projeções calculadas a partir das transações e posições próprias |

O proprietário nunca é escolhido pelo cliente. O JWT é validado pelo filtro de autenticação, o principal é colocado no `SecurityContext` e `UsuarioAtualService` fornece o usuário para `CarteiraService`. DTOs públicos de carteira não possuem `usuarioId`.

### Dados legados

Posições e transações anteriores à autenticação não têm proprietário confiável. Elas foram preservadas com `usuario_id NULL`, não são retornadas nas consultas autenticadas e não foram atribuídas pelo primeiro usuário, e-mail ou data.

Uma associação futura deve ser administrativa, explícita e auditada: escolher o usuário, fazer backup, analisar conflitos de posição/idempotência, atualizar somente registros nulos em transação e validar os resultados antes de considerar `usuario_id NOT NULL`.

## Autenticação e autorização

- senhas são armazenadas somente como hash BCrypt;
- cadastro público cria sempre o perfil `USER`;
- login emite JWT assinado com segredo externo;
- a API é stateless e não cria sessão HTTP;
- token inválido, expirado ou usuário inativo resulta em 401 JSON;
- `/auth/register`, `/auth/login`, preflight CORS e health são públicos;
- ações, corretoras, carteira e `/auth/me` exigem Bearer JWT;
- `/actuator/info` exige autenticação; demais endpoints Actuator são negados.

Não há refresh token, recuperação de senha, MFA ou autorização administrativa.

## Fluxo financeiro

### Compra

O serviço bloqueia pessimisticamente a ação durante a operação, localiza a posição pela chave `(usuario, acao)` e calcula o preço médio ponderado:

```text
novo preço médio =
  (quantidade atual × preço médio atual + quantidade comprada × preço de compra)
  / quantidade total
```

Uma transação de compra e a posição do mesmo usuário são persistidas na mesma transação de banco.

### Venda

A venda consulta exclusivamente a posição do usuário autenticado. Não é possível consumir saldo de outra conta. Na venda parcial, o preço médio restante não muda.

```text
resultado realizado =
  (preço de venda - preço médio anterior) × quantidade vendida
```

### Avaliação e moedas

```text
valor investido = preço médio × quantidade
valor atual = cotação atual × quantidade
resultado não realizado = valor atual - valor investido
rentabilidade (%) = resultado não realizado × 100 / valor investido
resultado total = resultado realizado + resultado não realizado
```

Quando a cotação está ausente, valores dependentes dela permanecem nulos. BRL e USD são agregados separadamente; não existe conversão cambial implícita.

## Idempotência e concorrência

`Idempotency-Key` é opcional nas compras e vendas, limitado a 100 caracteres e único por usuário. A mesma chave e a mesma operação produzem replay seguro; reutilização incompatível pelo mesmo usuário é rejeitada. Usuários diferentes podem usar a mesma chave.

O lock pessimista da ação serializa alterações concorrentes relevantes e a constraint `(usuario_id, acao_id)` impede posições duplicadas. A operação de banco é transacional.

## Persistência e Liquibase

O Liquibase é a fonte do schema. As migrations são aditivas e o Hibernate apenas valida o mapeamento. Os testes usam H2 em modo PostgreSQL para cenários rápidos e Testcontainers/PostgreSQL 17 para comportamento específico de constraints, locks e migrations.

## Integrações externas

- ViaCEP: endereço de corretoras;
- BrasilAPI: dados de CNPJ e registro CVM;
- Brapi: dados e cotações de ativos brasileiros;
- Twelve Data: dados e cotações de ativos norte-americanos.

Tokens externos são recebidos por variáveis de ambiente. Os clientes usam timeouts explícitos de conexão e leitura, configuráveis por ambiente. Exceções dos provedores são traduzidas para Problem Details sem devolver credenciais ou detalhes internos.

## Observabilidade e segurança HTTP

`RequestObservabilityFilter` aceita apenas `X-Request-Id` curto e com caracteres seguros; valores ausentes ou inválidos são substituídos por UUID. O identificador é devolvido na resposta, colocado no MDC durante o processamento e removido no `finally`.

O log por requisição contém somente request ID, método, path sem query string, status e duração. Não são registrados Authorization, JWT, cookies, corpos, senhas ou chaves externas.

O Actuator expõe somente `health` e `info`. Health é público e não mostra componentes ou detalhes internos. Info contém metadados mínimos e exige JWT. `SecurityHeadersFilter`, CORS explícito e respostas Problem Details continuam ativos.
