# Gestão de Ações

Aplicação web para cadastro de ativos e corretoras, registro de compras e vendas e acompanhamento de carteiras de ações em BRL e USD. O sistema possui autenticação JWT stateless e mantém posições, transações, histórico e resultados financeiros isolados por usuário.

## Funcionalidades

- cadastro e login de usuários com senha protegida por BCrypt;
- sessão frontend com JWT Bearer armazenado em `sessionStorage`;
- cadastro global de ações brasileiras e norte-americanas;
- cadastro global de corretoras com validação de CEP, CNPJ e CVM;
- atualização de cotações por provedores externos;
- compra e venda com idempotência e controle de concorrência;
- posições, histórico e resumo financeiro exclusivos do usuário autenticado;
- resultados realizados e não realizados separados por BRL e USD;
- interface React responsiva com temas claro e escuro;
- OpenAPI/Swagger, health check e logs HTTP com `X-Request-Id`.

## Arquitetura e tecnologias

O frontend React/Vite consome a API HTTP/JSON Spring Boot. O backend organiza o fluxo em controllers, services e repositories, persiste em PostgreSQL e versiona o schema com Liquibase. Integrações externas são acessadas por clientes OpenFeign.

- Java 21 e Spring Boot 3.3.0;
- Spring Web, Validation, Data JPA, Security e Actuator;
- JWT com JJWT e BCrypt;
- PostgreSQL 17, H2 para testes e Liquibase;
- Spring Cloud OpenFeign;
- Springdoc OpenAPI/Swagger UI;
- React 19 e Vite;
- JUnit 5, Mockito e Testcontainers;
- Docker e Docker Compose.

As integrações externas usam timeouts explícitos de conexão e leitura (3 s e 5 s por padrão),
configuráveis por `EXTERNAL_API_CONNECT_TIMEOUT_MS` e `EXTERNAL_API_READ_TIMEOUT_MS`.

Detalhes estão em [Arquitetura](docs/ARQUITETURA.md) e [API](docs/API.md).

## Requisitos

Para a execução completa com containers:

- Docker Desktop com Docker Compose;
- portas 5173, 8080 e 5432 livres, ou portas alternativas no `.env`.

Para desenvolvimento sem container:

- JDK 21 ou superior;
- Node.js compatível com Vite 8 e npm;
- PostgreSQL 17 acessível ao backend.

O Maven Wrapper está versionado; não é necessário instalar Maven globalmente.

## Configuração

Copie `.env.example` para `.env` apenas no ambiente local e substitua os placeholders. O `.env` real não deve ser versionado.

Variáveis principais:

| Variável | Finalidade |
|---|---|
| `SPRING_PROFILES_ACTIVE` | Profile Spring, normalmente `dev` localmente ou `prod` em produção |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Conexão direta do backend ao PostgreSQL |
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | Inicialização do PostgreSQL no Compose |
| `BRAPI_TOKEN` | Token da Brapi |
| `TWELVE_DATA_API_KEY` | Chave da Twelve Data |
| `JWT_SECRET` | Segredo externo de pelo menos 32 bytes para assinatura JWT |
| `JWT_EXPIRATION_MINUTES` | Duração do access token em minutos |
| `CORS_ALLOWED_ORIGINS` | Origens permitidas, separadas por vírgula; `*` não é aceito |
| `SWAGGER_ENABLED` | Habilita Swagger no profile `prod`; padrão seguro `false` |
| `APP_PORT`, `FRONTEND_PORT`, `POSTGRES_PORT` | Portas publicadas pelo Compose |

Não reutilize os placeholders do exemplo em produção e nunca coloque segredos em `application.yml`, código-fonte ou documentação.

## Execução com Docker Compose

1. Configure o `.env` local.
2. Inicie o Docker Desktop.
3. Na raiz do projeto, execute:

```bash
docker compose up -d --build
```

Serviços locais:

| Serviço | URL |
|---|---|
| Frontend | http://localhost:5173 |
| Backend | http://localhost:8080 |
| Health | http://localhost:8080/actuator/health |
| Swagger UI em `dev` | http://localhost:8080/swagger-ui/index.html |
| OpenAPI JSON em `dev` | http://localhost:8080/v3/api-docs |

O backend só é considerado saudável quando `/actuator/health` responde com `UP`. Para acompanhar os serviços:

```bash
docker compose ps
docker compose logs -f aplicacao
```

Para parar sem apagar dados:

```bash
docker compose down
```

`docker compose down -v` remove os volumes e os dados do PostgreSQL. Não use essa opção casualmente.

## Backend sem Docker

Com PostgreSQL disponível, configure `SPRING_PROFILES_ACTIVE=dev`, `DB_PASSWORD`, `BRAPI_TOKEN`, `TWELVE_DATA_API_KEY` e `JWT_SECRET`. `DB_URL`, `DB_USERNAME`, `JWT_EXPIRATION_MINUTES` e `CORS_ALLOWED_ORIGINS` possuem configurações locais ajustáveis.

Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Linux/macOS:

```bash
./mvnw spring-boot:run
```

O Spring Boot não lê `.env` automaticamente quando executado diretamente; exporte as variáveis no terminal ou configure-as na IDE.

## Frontend em desenvolvimento

```bash
cd front-carteira
npm install
npm run dev
```

`VITE_API_URL` pode apontar para outra URL do backend. Sem configuração, o frontend usa `http://localhost:8080`.

## Autenticação e isolamento

Crie uma conta pela tela `/cadastro` ou por `POST /auth/register`. Cadastro e login devolvem um access token JWT; o frontend o mantém somente na sessão do navegador. Não há refresh token nem logout server-side.

Todas as APIs de ações, corretoras e carteira exigem `Authorization: Bearer <token>`. Ações e corretoras são referências globais. Posições, transações, histórico, resumo e resultados pertencem ao usuário obtido do JWT/SecurityContext. A API nunca aceita `usuarioId` do cliente para escolher o proprietário da carteira.

Registros financeiros anteriores ao isolamento permanecem no banco com `usuario_id NULL`, não foram atribuídos automaticamente e não aparecem nas consultas autenticadas.

## Regras financeiras

- compra: preço médio ponderado pela quantidade;
- venda parcial: o preço médio da posição restante não muda;
- resultado realizado: `(preço de venda - preço médio anterior) × quantidade vendida`;
- resultado não realizado: `valor atual - valor investido`;
- rentabilidade: `(resultado não realizado × 100) / valor investido`, retornando zero quando o valor investido é zero;
- rentabilidade realizada de uma venda: `(preço de venda - preço médio anterior) × 100 / preço médio anterior`;
- valores BRL e USD são resumidos separadamente e não são somados entre moedas.

## APIs externas

| Provedor | Uso | Credencial |
|---|---|---|
| ViaCEP | endereço por CEP | não exige |
| BrasilAPI | CNPJ e situação de corretora na CVM | não exige |
| Brapi | ações e cotações brasileiras | `BRAPI_TOKEN` |
| Twelve Data | ações e cotações norte-americanas | `TWELVE_DATA_API_KEY` |

Falhas externas são convertidas em Problem Details seguros, sem refletir URLs, tokens ou detalhes internos.

## Banco e migrations

O Liquibase executa os changeSets de `src/main/resources/db/changelog`. O profile `dev` pode carregar dados de demonstração pelo contexto `demo`; `prod` não carrega esse seed. O Hibernate usa `ddl-auto=validate` e não substitui as migrations.

## OpenAPI e observabilidade

O Swagger declara autenticação HTTP Bearer JWT e oferece o botão **Authorize**. `/auth/register` e `/auth/login` continuam públicos. Swagger permanece habilitado em `dev/test` e desabilitado por padrão em `prod`.

- `GET /actuator/health`: público, retorna apenas o estado geral;
- `GET /actuator/info`: exige autenticação e fornece metadados mínimos;
- outros endpoints Actuator não são expostos e são negados pela segurança;
- toda resposta recebe `X-Request-Id`;
- cada requisição gera um log com request ID, método, path sem query string, status e duração.

Authorization, JWT, cookies, senhas, corpos de autenticação e segredos não são registrados.

## Testes

Backend, com JDK 21:

```powershell
.\mvnw.cmd test
```

```bash
./mvnw test
```

A suíte inclui testes unitários, HTTP, migrations, segurança, isolamento multiusuário e PostgreSQL real via Testcontainers.

Frontend:

```bash
cd front-carteira
npm test
npm run lint
npm run build
```

## Estrutura principal

```text
src/main/java/...        controllers, services, repositories, segurança e integrações
src/main/resources/      configurações e changelogs Liquibase
src/test/                testes backend e Testcontainers
front-carteira/          frontend React/Vite
docs/                    arquitetura e referência da API
graphify-out/            grafo de conhecimento do projeto
compose.yaml             stack local PostgreSQL, backend e frontend
```

## Limitações atuais

O sistema não implementa impostos, taxas, dividendos/proventos, conversão cambial, refresh token, recuperação de senha, MFA, permissões administrativas, auditoria completa de banco ou tracing distribuído. A disponibilidade e os limites dos provedores externos também afetam atualizações cadastrais e de cotação.
