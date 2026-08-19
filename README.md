# Sistema de Gestão de Ações (OpenSpec MVP)

API REST desenvolvida em Java 21 com Spring Boot 3 para o cadastro e gestão de Corretoras e Ações financeiras, consumindo dados reais de APIs públicas e privadas.

## 🚀 Tecnologias Utilizadas

* **Java 21 LTS**
* **Spring Boot 3.3.0** (Web, Data JPA, Validation)
* **Spring Cloud OpenFeign** (Integrações REST HTTP)
* **PostgreSQL & H2 Database** (Persistência)
* **Flyway** (Migrations e Versionamento de Banco)
* **JUnit 5 & Mockito** (Testes Automatizados)
* **Springdoc OpenAPI** (Swagger UI para Documentação)

## ⚙️ Configuração e Execução (Desenvolvimento)

O projeto está configurado para rodar imediatamente utilizando um banco de dados H2 em memória. Nenhuma infraestrutura adicional é necessária para iniciar a aplicação localmente.

1. Clone o repositório.
2. Abra o projeto no IntelliJ IDEA.
3. Aguarde o Maven baixar as dependências (`pom.xml`).
4. Execute a classe principal: `GestaoAcoesApplication.java`.

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

Para o MVP e testes locais, as chaves das APIs financeiras não são obrigatórias, mas para operações intensivas, você deve configurar as seguintes variáveis (ou adicioná-las no `application.yml`):

* `app.market-data.brapi.token`: Seu token da Brapi.
* `app.market-data.twelve-data.apikey`: Sua API Key da Twelve Data.

**Atenção:** Nunca versione segredos no código-fonte.

## 🧪 Testes

Os testes automatizados foram construídos usando JUnit 5 e Mockito. Eles simulam as respostas das APIs externas para garantir que a suíte de testes seja rápida, determinística e **não consuma as cotas** dos provedores de dados do mercado financeiro.