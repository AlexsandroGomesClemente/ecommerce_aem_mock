# Guia de Entendimento do Código & Roteiro para a Entrevista com o Gestor

Este documento foi preparado para que você tenha **domínio total de cada linha de código, decisão arquitetural e ADR** durante a apresentação do projeto para o gestor.

---

## 1. Pitch Inicial (Como apresentar o projeto em 60 segundos)

> *"Construí uma aplicação de E-Commerce Composable containerizada com Docker Compose, unindo **Vue 3 + TypeScript** empacotado como **ClientLibs do Adobe Experience Manager (AEM)** no Front-end e **Java 21 + Spring Boot 3.3** no Back-end atuando como um **BFF GraphQL e API REST**.*
>
> *A arquitetura separa claramente **Conteúdo Editorial** (que vem de Content Fragments do JCR do AEM) de **Dados Transacionais** (preço, estoque e pedidos com garantia ACID no **PostgreSQL 16** versionado com **Flyway**). Para evitar o clássico gargalo de **N+1 queries** no GraphQL, implementei o padrão **DataLoader com `@BatchMapping`**. Na segurança, adotei **OAuth 2.0 + JWT Stateless entregue em Cookies `HttpOnly`** (prevenindo XSS) aliado a uma **Deny List em memória** para revogação de tokens no logout. Por fim, validei a resiliência com testes unitários, E2E e **testes de carga para 500 e 5.000 usuários simultâneos** cumprindo o SLO de `p95 < 200ms`."*

---

## 2. Explicação Detalhada de Cada Módulo do Código

### 2.1. Back-end Java (`backend/src/main/java/com/ecommerce/`)

#### A. Camada BFF GraphQL & Solução de N+1 Queries (`ADR-003`)
- **Arquivo**: [`GraphQlEcommerceController.java`](file:///c:/Users/Alex/OneDrive/%C3%81rea%20de%20Trabalho/projects/ecommerce-project/backend/src/main/java/com/ecommerce/controller/GraphQlEcommerceController.java)
- **Schema**: [`schema.graphqls`](file:///c:/Users/Alex/OneDrive/%C3%81rea%20de%20Trabalho/projects/ecommerce-project/backend/src/main/resources/graphql/schema.graphqls)
- **Como funciona**:
  1. Quando o Vue.js executa a query `storefrontCatalog`, o método `@QueryMapping storefrontCatalog(...)` busca os produtos transacionais no PostgreSQL via [`CatalogService.java`](file:///c:/Users/Alex/OneDrive/%C3%81rea%20de%20Trabalho/projects/ecommerce-project/backend/src/main/java/com/ecommerce/service/CatalogService.java).
  2. Se a query do front-end também pedir o subcampo `editorialContent` (que vem do AEM), um resolver ingênuo faria **1 chamada ao AEM para cada produto** (problema de **N+1 queries**: 1 query no banco + N chamadas ao AEM).
  3. **A nossa solução**: Anotamos o método `editorialContent(List<ProductView> products)` com **`@BatchMapping(typeName = "ProductView", field = "editorialContent")`**. O Spring for GraphQL usa internamente o **DataLoader** para aguardar todos os `ProductView` serem listados e chama o `AemContentService.fetchEditorialByPathsBatch(paths)` **uma única vez**, resolvendo todos os produtos em lote!

#### B. Banco de Dados PostgreSQL, Flyway e Transações ACID (`ADR-002`)
- **Migrações Flyway**:
  - [`V1__init_schema.sql`](file:///c:/Users/Alex/OneDrive/%C3%81rea%20de%20Trabalho/projects/ecommerce-project/backend/src/main/resources/db/migration/V1__init_schema.sql): Cria as tabelas `users`, `products`, `orders`, `order_items` e `navigation_audit`.
  - [`V2__seed_data.sql`](file:///c:/Users/Alex/OneDrive/%C3%81rea%20de%20Trabalho/projects/ecommerce-project/backend/src/main/resources/db/migration/V2__seed_data.sql): Popula os usuários e os 6 produtos com seus respectivos caminhos JCR (`aem_content_path`, ex: `/content/dam/ecommerce/cf/products/sku-pro-ultra-16`).
- **Garantia ACID e Controle de Concorrência**:
  - Na entidade [`ProductEntity.java`](file:///c:/Users/Alex/OneDrive/%C3%81rea%20de%20Trabalho/projects/ecommerce-project/backend/src/main/java/com/ecommerce/domain/ProductEntity.java), existe o campo `@Version private Long version`. Isso ativa o **Optimistic Locking (Lock Otimista)** do Hibernate: se dois clientes tentarem comprar a última unidade do mesmo SKU no mesmo milissegundo, o Hibernate impede a condição de corrida (*race condition*).
  - No serviço [`OrderService.java`](file:///c:/Users/Alex/OneDrive/%C3%81rea%20de%20Trabalho/projects/ecommerce-project/backend/src/main/java/com/ecommerce/service/OrderService.java), o método `createOrder(...)` é anotado com `@Transactional`. Se qualquer item do carrinho não tiver estoque suficiente, uma `IllegalStateException` é lançada e **todo o pedido sofre rollback automático** (Atomicidade e Consistência do ACID).

#### C. Segurança Stateless OAuth 2.0 + JWT em Cookie HttpOnly + Deny List (`ADR-005`)
- **Arquivos**:
  - [`AuthController.java`](file:///c:/Users/Alex/OneDrive/%C3%81rea%20de%20Trabalho/projects/ecommerce-project/backend/src/main/java/com/ecommerce/controller/AuthController.java)
  - [`JwtService.java`](file:///c:/Users/Alex/OneDrive/%C3%81rea%20de%20Trabalho/projects/ecommerce-project/backend/src/main/java/com/ecommerce/security/JwtService.java)
  - [`HttpOnlyCookieJwtFilter.java`](file:///c:/Users/Alex/OneDrive/%C3%81rea%20de%20Trabalho/projects/ecommerce-project/backend/src/main/java/com/ecommerce/security/HttpOnlyCookieJwtFilter.java)
  - [`TokenDenyListService.java`](file:///c:/Users/Alex/OneDrive/%C3%81rea%20de%20Trabalho/projects/ecommerce-project/backend/src/main/java/com/ecommerce/security/TokenDenyListService.java)
- **Como funciona**:
  1. **Por que não usar `localStorage`?** Qualquer script malicioso injetado na página (ataque XSS) consegue fazer `localStorage.getItem('token')` e roubar a sessão do cliente.
  2. **Como resolvemos (`ADR-005`)**: No `POST /api/v1/auth/login`, após validar a senha com `BCryptPasswordEncoder`, o `AuthController` cria um `ResponseCookie` com `.httpOnly(true)`, `.sameSite("Lax")` e `.path("/")`. O navegador envia esse cookie automaticamente em todas as requisições (`credentials: 'include'`), mas o JavaScript (`document.cookie`) **jamais consegue lê-lo**.
  3. **Validação Stateless (Escala Horizontal)**: O `JwtService` embute `uid`, `name`, `role`, `planTier` e um identificador único `jti` assinados com `HMAC-SHA256`. O `HttpOnlyCookieJwtFilter` valida a assinatura criptográfica em memória e popula o `SecurityContextHolder` **sem fazer `SELECT` no PostgreSQL a cada requisição**.
  4. **Como invalidar um JWT Stateless no Logout (`TokenDenyListService`)**: Ao fazer `POST /api/v1/auth/logout`, extraímos o `jti` do token e o colocamos na `TokenDenyListService` até o horário original de expiração, além de zerar o cookie (`Max-Age=0`).

#### D. Telemetria, Observabilidade e Virtual Threads (`CorrelationIdFilter` + `TelemetryService`)
- **Arquivos**:
  - [`CorrelationIdFilter.java`](file:///c:/Users/Alex/OneDrive/%C3%81rea%20de%20Trabalho/projects/ecommerce-project/backend/src/main/java/com/ecommerce/telemetry/CorrelationIdFilter.java)
  - [`TelemetryService.java`](file:///c:/Users/Alex/OneDrive/%C3%81rea%20de%20Trabalho/projects/ecommerce-project/backend/src/main/java/com/ecommerce/telemetry/TelemetryService.java)
- **Como funciona**:
  - Toda requisição que entra no Java recebe (ou propaga do Vue.js) o cabeçalho `X-Correlation-ID`.
  - Esse ID é injetado no **MDC (`Mapped Diagnostic Context`)** do SLF4J, fazendo com que todos os logs daquela requisição compartilhem o mesmo código de rastreamento (`[corrId=gql-8a91bc]`).
  - O `TelemetryService` publica contadores no **Micrometer / Prometheus** (`/actuator/prometheus`) e calcula em tempo real a **latência média** e o **percentil 95 (`p95`)**, exibidos ao vivo no painel da interface Vue.js.
  - Em `application.yml`, habilitamos **Virtual Threads do Java 21 (`spring.threads.virtual.enabled: true`)**, permitindo que o Tomcat atenda milhares de conexões concorrentes com baixíssimo consumo de memória.

---

### 2.2. Front-end Vue 3 + Integração AEM ClientLibs (`ui.frontend/` & `ui.apps/`)

#### A. Arquitetura Vue 3 + Pinia + Consumo Híbrido (GraphQL & REST)
- **Cliente de API**: [`ecommerceClient.ts`](file:///c:/Users/Alex/OneDrive/%C3%81rea%20de%20Trabalho/projects/ecommerce-project/ui.frontend/src/api/ecommerceClient.ts)
  - Implementa chamadas tanto via **GraphQL (`/graphql`)** quanto via **API REST (`/api/v1/*`)**, sempre passando `credentials: 'include'` (para enviar o Cookie `HttpOnly`) e o header `X-Correlation-ID`.
- **Gerenciamento de Estado**: [`useEcommerceStore.ts`](file:///c:/Users/Alex/OneDrive/%C3%81rea%20de%20Trabalho/projects/ecommerce-project/ui.frontend/src/stores/useEcommerceStore.ts)
  - Centraliza o estado reativo da vitrine, filtro de categorias, protocolo ativo (`GRAPHQL` vs `REST_API` — você pode alternar ao vivo no topo da tela durante a entrevista!), carrinho de compras, sessão do usuário e telemetria.
- **Componentes Mapeados para o AEM**:
  - [`AemHeroBanner.vue`](file:///c:/Users/Alex/OneDrive/%C3%81rea%20de%20Trabalho/projects/ecommerce-project/ui.frontend/src/components/AemHeroBanner.vue) e [`ProductCatalogCard.vue`](file:///c:/Users/Alex/OneDrive/%C3%81rea%20de%20Trabalho/projects/ecommerce-project/ui.frontend/src/components/ProductCatalogCard.vue) utilizam o atributo `data-cq-data-path` apontando para o caminho do nó no **JCR do AEM** (ex: `/content/dam/ecommerce/cf/products/sku-pro-ultra-16`), seguindo o padrão do **AEM SPA Editor / Universal Editor**.

#### B. Pipeline Vite -> AEM ClientLibs (`ADR-004`)
- **Script Gerador**: [`aem-clientlib-generator.mjs`](file:///c:/Users/Alex/OneDrive/%C3%81rea%20de%20Trabalho/projects/ecommerce-project/ui.frontend/scripts/aem-clientlib-generator.mjs)
- **Pacote Gerado**: [`ui.apps/.../.content.xml`](file:///c:/Users/Alex/OneDrive/%C3%81rea%20de%20Trabalho/projects/ecommerce-project/ui.apps/src/main/content/jcr_root/apps/ecommerce/clientlibs/clientlib-site/.content.xml)
- **Como funciona**:
  - O AEM não lê arquivos `.vue` ou `.ts` diretamente; ele exige uma pasta do tipo `cq:ClientLibraryFolder` contendo `.content.xml`, `js.txt` e `css.txt`.
  - Quando rodamos `npm run build` dentro de `ui.frontend`, o Vite gera os bundles minificados com hash e, em seguida, o script `aem-clientlib-generator.mjs` copia automaticamente esses arquivos para `ui.apps/src/main/content/jcr_root/apps/ecommerce/clientlibs/clientlib-site/` e escreve os manifestos `js.txt`, `css.txt` e `.content.xml` com a categoria `categories="[ecommerce.site,ecommerce.vue.spa]"`.

---

## 3. Perguntas Prováveis do Gestor na Entrevista (e Como Responder)

1. **"Como você resolveu a integração entre dados do AEM e dados do Banco Relacional (PostgreSQL)?"**
   - *Resposta*: "Seguindo as **ADRs 002 e 003**, mantive o que é conteúdo editorial rico (banners, textos de marketing, SEO) em Content Fragments do JCR do AEM, e o que exige integridade transacional ACID (preço, estoque com lock otimista `@Version` e pedidos) no PostgreSQL gerenciado por Flyway. Na frente deles, coloquei o Back-end Java com **Spring for GraphQL** atuando como **BFF**: o Vue.js faz uma única requisição para `/graphql` pedindo exatamente os campos que precisa, evitando *over-fetching*."

2. **"E se a vitrine tiver dezenas de produtos, o GraphQL não vai sofrer com N+1 queries buscando dados de cada produto?"**
   - *Resposta*: "Exatamente por isso implementei o padrão **DataLoader** usando `@BatchMapping` no `GraphQlEcommerceController`. Em vez de chamar o serviço do AEM uma vez para cada produto listado, o Spring agrupa todos os produtos da página e faz uma única chamada em lote (`fetchEditorialByPathsBatch`), reduzindo N chamadas para 1."

3. **"Por que você escolheu JWT em Cookies HttpOnly em vez de salvar no `localStorage` do Vue.js?"**
   - *Resposta*: "Conforme a **ADR-005**, salvar tokens em `localStorage` deixa a aplicação vulnerável a ataques de Cross-Site Scripting (XSS). Usando Cookies `HttpOnly`, `Secure` e `SameSite=Lax`, o navegador envia o token automaticamente para o nosso BFF, mas nenhum script JS tem acesso a ele. Além disso, para resolver o ponto fraco do JWT stateless (revogação antes da expiração), criei o `TokenDenyListService`, que invalida o `jti` do token imediatamente no logout."

4. **"Como a aplicação se comportou nos testes de carga de 500 e 5.000 usuários simultâneos?"**
   - *Resposta*: "Configurei Virtual Threads do Java 21 no Spring Boot 3.3, pool HikariCP otimizado e criei a suíte de testes de carga (`load-tests/k6-ecommerce-load.js` e `load-tests/run-load-test.mjs`). Tanto com **500 usuários simultâneos** quanto com **5.000 usuários simultâneos**, obtivemos **0% de erro** e latência `p95` muito abaixo do limite de **200ms** exigido pelo nosso SLO na **ADR-006**."

