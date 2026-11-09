# QuantumStore Enterprise — E-Commerce (Vue.js 3 + AEM + Java Spring Boot + GraphQL + PostgreSQL)

Aplicação de **E-Commerce Enterprise** desenvolvida para apresentação técnica em entrevista com gestor, atendendo integralmente à especificação em [`docs/introducao.md`](file:///c:/Users/Alex/OneDrive/%C3%81rea%20de%20Trabalho/projects/ecommerce-project/docs/introducao.md) e às decisões arquiteturais de [`ADR-001`](file:///c:/Users/Alex/OneDrive/%C3%81rea%20de%20Trabalho/projects/ecommerce-project/docs/ADRS/ADR001.md) até [`ADR-006`](file:///c:/Users/Alex/OneDrive/%C3%81rea%20de%20Trabalho/projects/ecommerce-project/docs/ADRS/ADR006.md).

---

## 📚 Documentação Principal para a Entrevista

1. 📋 **[Plano de Desenvolvimento (`docs/PLANS/PLANO_DESENVOLVIMENTO.md`)](file:///c:/Users/Alex/OneDrive/%C3%81rea%20de%20Trabalho/projects/ecommerce-project/docs/PLANS/PLANO_DESENVOLVIMENTO.md)**
2. 📊 **[Fluxogramas da Arquitetura para Apresentação (`docs/FLUXOGRAMA_ARQUITETURA.md`)](file:///c:/Users/Alex/OneDrive/%C3%81rea%20de%20Trabalho/projects/ecommerce-project/docs/FLUXOGRAMA_ARQUITETURA.md)**
3. 🧠 **[Guia de Entendimento do Código & Perguntas da Entrevista (`docs/GUIA_ENTREVISTA_ENTENDIMENTO_CODIGO.md`)](file:///c:/Users/Alex/OneDrive/%C3%81rea%20de%20Trabalho/projects/ecommerce-project/docs/GUIA_ENTREVISTA_ENTENDIMENTO_CODIGO.md)**
4. 🚀 **[Relatório de Testes de Carga — 500 e 5.000 Usuários Simultâneos (`load-tests/RELATORIO_TESTE_CARGA.md`)](file:///c:/Users/Alex/OneDrive/%C3%81rea%20de%20Trabalho/projects/ecommerce-project/load-tests/RELATORIO_TESTE_CARGA.md)**

---

## 🛠️ Stack Tecnológica & Mapeamento das ADRs

| Camada | Tecnologia | ADR Relacionada |
| :--- | :--- | :--- |
| **Front-end & CMS** | **Vue 3 + TypeScript + Vite + Pinia** empacotado como **AEM ClientLibs** (`ui.apps/.../.content.xml`, `js.txt`, `css.txt`) + Componentes mapeados ao JCR (`data-cq-data-path`) | **ADR-004** |
| **Back-end BFF** | **Java 21/22 (Virtual Threads) + Spring Boot 3.3**, expondo **Spring for GraphQL (`/graphql`)** com **`@BatchMapping` (DataLoader anti N+1)** e **API REST (`/api/v1/*`)** | **ADR-003** |
| **Banco de Dados** | **PostgreSQL 16** + **Spring Data JPA / Hibernate** + **Flyway** (`V1__init_schema.sql`, `V2__seed_data.sql`) com transações **ACID** (`@Transactional`) e Lock Otimista (`@Version`) | **ADR-002** |
| **Segurança** | **Spring Security 6 + OAuth 2.0 / JWT Stateless** entregue via **Cookies `HttpOnly; SameSite=Lax`** (proteção contra XSS) + **Token Deny List** em memória para revogação imediata no logout | **ADR-005** |
| **Testes & Carga** | **JUnit 5 + MockMvc** (Back-end), **Vitest** (Front-end), **Playwright** (E2E) e **Grafana k6 + Node Load Runner** para **500 e 5.000 usuários simultâneos** (SLO `p95 < 200ms`) | **ADR-006** |
| **Telemetria** | **SLF4J + MDC (`X-Correlation-ID`)**, **Spring Boot Actuator + Micrometer Prometheus** (`/actuator/prometheus`) e Dashboard em tempo real na UI | **Telemetria** |
| **Containerização** | **Docker & Docker Compose** orquestrando PostgreSQL, AEM Mock, Back-end Java, Front-end Nginx e Prometheus | **ADR-001** |

---

## ▶️ Como Executar a Aplicação

### Opção 1: Via Docker Compose (Ecossistema Completo — ADR-001)
```bash
docker compose up --build
```
- **Front-end Vue + AEM ClientLibs**: `http://localhost`
- **Back-end Java (GraphQL & REST)**: `http://localhost:8080/graphql` e `http://localhost:8080/api/v1/products`
- **Prometheus Metrics**: `http://localhost:9090`

### Opção 2: Execução Local Rápida (Sem precisar do Docker Desktop aberto)

1. **Subir o Back-end Java (Spring Boot 3.3)**:
   ```powershell
   cd backend
   .\mvnw.cmd spring-boot:run
   ```
2. **Subir o Front-end Vue 3 + Vite**:
   ```powershell
   cd ui.frontend
   npm run dev
   ```
   Acesse `http://localhost:5173` (Credenciais de demonstração: `alex@ecommerce.com` / `Senha@123`).

---

## 🧪 Como Executar os Testes

1. **Testes Unitários e de Integração do Back-end (JUnit 5)**:
   ```powershell
   cd backend
   .\mvnw.cmd test
   ```
2. **Testes Unitários do Front-end (Vitest) + Geração de AEM ClientLibs (ADR-004)**:
   ```powershell
   cd ui.frontend
   npm run test:unit
   npm run build
   ```
3. **Testes de Carga (500 e 5.000 Usuários Simultâneos — ADR-006)**:
   ```powershell
   # Com o back-end rodando na porta 8080:
   node load-tests/run-load-test.mjs
   # Ou com o binário k6 instalado:
   k6 run load-tests/k6-ecommerce-load.js
   ```

