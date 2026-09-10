# Sistema de Gestão de Ações (OpenSpec MVP)

API REST desenvolvida em Java 21 com Spring Boot 3 para o cadastro e gestão de Corretoras e Ações financeiras, consumindo dados reais de APIs públicas e privadas.

## 🚀 Tecnologias Utilizadas

* **Java 21 LTS**
* **Spring Boot 3.3.0** (Web, Data JPA, Validation)
* **Spring Cloud OpenFeign** (Integrações REST HTTP)
* **PostgreSQL & H2 Database** (Persistência)
* **Liquibase** (Migrations e Versionamento de Banco)
* **JUnit 5 & Mockito** (Testes Automatizados)
* **Springdoc OpenAPI** (Swagger UI para Documentação)

## Requisitos e build

Para executar localmente, instale um **JDK 21 ou superior** e configure `JAVA_HOME` para
essa instalação. O Maven Wrapper versionado no projeto fixa a versão do Maven e deve ser
preferido no lugar de uma instalação global.

No Windows:

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

No Linux/macOS:

```bash
./mvnw test
./mvnw spring-boot:run
```

O build via Docker usa Maven e Java 21 dentro da própria imagem, sem depender do JDK ou
do Maven instalados na máquina host.

## ⚙️ Configuração e Execução (Desenvolvimento)

O profile `dev` usa PostgreSQL e carrega os dados de demonstração do Liquibase. Defina
`SPRING_PROFILES_ACTIVE=dev`, `DB_PASSWORD`, `BRAPI_TOKEN` e `TWELVE_DATA_API_KEY` no
ambiente do processo. `DB_URL` e `DB_USERNAME` são opcionais no desenvolvimento e usam,
respectivamente, `jdbc:postgresql://localhost:5432/gestao_acoes` e `postgres` como padrão.

O Spring Boot não carrega o arquivo `.env` automaticamente. Em execução direta via Maven
ou IDE, configure as variáveis no terminal ou na configuração de execução. Para executar
com Docker, copie `.env.example` para `.env`, substitua todos os placeholders e use
`docker compose up --build`; o Compose lê o `.env` e repassa as variáveis ao backend.

A aplicação estará disponível em: `http://localhost:8080`

## 📚 Documentação da API (Swagger)

A documentação interativa de todos os endpoints, parâmetros e modelos de resposta está disponível via Swagger UI.

Com a aplicação rodando, acesse:
👉 **[http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)**

## 🔌 Integrações Externas (APIs)

A aplicação consome as seguintes APIs isoladas por adaptadores (Ports and Adapters):

| Serviço | Provedor | Finalidade | Autenticação |
|---|---|---|---|
| **CEP** | [ViaCEP](https://viacep.com.br/) | Busca de endereço | Nenhuma |
| **CNPJ/CVM** | [BrasilAPI](https://brasilapi.com.br/) | Validação de Corretora | Nenhuma |
| **Ações BR** | [Brapi](https://brapi.dev/) | Cotações do mercado brasileiro | Token via query param |
| **Ações US** | [Twelve Data](https://twelvedata.com/) | Cotações do mercado americano | API Key via query param |

*Nota sobre Limites (Rate Limits): As APIs de cotações financeiras possuem limites estritos no plano gratuito. Se o limite for excedido, a aplicação retornará um erro amigável (`Problem Details`) informando a indisponibilidade.*

## 🔒 Variáveis de Ambiente

Os profiles `dev` e `prod` recebem credenciais exclusivamente por configuração externa:

* `DB_URL`, `DB_USERNAME` e `DB_PASSWORD`: conexão PostgreSQL (`prod` exige as três);
* `BRAPI_TOKEN`: token da Brapi;
* `TWELVE_DATA_API_KEY`: chave da Twelve Data.

O profile `prod` não possui fallback para essas variáveis, desativa `show-sql` e não executa
os dados demo. Não coloque segredos no `application.yml`, no Compose ou no `.env.example`.

**Atenção:** Nunca versione segredos no código-fonte.

## 🧪 Testes

Os testes automatizados foram construídos usando JUnit 5 e Mockito. Eles simulam as respostas das APIs externas para garantir que a suíte de testes seja rápida, determinística e **não consuma as cotas** dos provedores de dados do mercado financeiro.

Execute o comando de teste do Maven Wrapper indicado acima. A suíte ativa automaticamente o profile `test`, usa um banco H2 isolado,
não carrega dados demo e não exige PostgreSQL nem credenciais de APIs externas.
