# Fluxograma da Aplicação — Apresentação para Entrevista com o Gestor

Este documento reúne os fluxogramas arquiteturais e de sequência da aplicação **QuantumStore E-Commerce**, projetados especificamente para apresentação técnica durante a entrevista com o gestor.

---

## 1. Visão Macro da Arquitetura (Ecossistema Containerizado — ADR-001 a ADR-006)

```mermaid
flowchart TB
    subgraph Browser["Camada Cliente (Navegador do Usuário)"]
        VUE["SPA Vue 3 + TypeScript + Pinia<br/>(Componentes mapeados com data-cq-data-path)"]
        COOKIE["Cookie HttpOnly + Secure + SameSite=Lax<br/>(Armazena JWT sem expor ao JavaScript / Anti-XSS)"]
    end

    subgraph AEM_Ecosystem["Adobe Experience Manager (ADR-001 / ADR-004)"]
        CLIENTLIBS["ui.apps / AEM ClientLibs<br/>(cq:ClientLibraryFolder: .content.xml, js.txt, css.txt)"]
        JCR["AEM JCR Repository (Content Fragments)<br/>Conteúdo Editorial: Hero Banner, Textos Ricos, Badges, SEO"]
    end

    subgraph Java_BFF["Back-end BFF em Java 21 + Spring Boot 3.3 (ADR-003 / ADR-005)"]
        CORR["CorrelationIdFilter (Telemetria)<br/>Gera X-Correlation-ID + MDC Logging + Micrometer"]
        JWT_FILTER["HttpOnlyCookieJwtFilter + TokenDenyListService<br/>Validação Criptográfica Stateless (Zero query de sessão)"]
        GQL["GraphQlEcommerceController (/graphql)<br/>Schema Tipado + @BatchMapping (DataLoader Anti N+1)"]
        REST["RestEcommerceController (/api/v1/*)<br/>Endpoints REST Híbridos (Auth, Catálogo, Pedidos, Telemetria)"]
        ORDER_SVC["OrderService (@Transactional ACID)<br/>Validação de Estoque + Lock Otimista (@Version)"]
    end

    subgraph Persistence_Observability["Dados Transacionais & Observabilidade (ADR-002 / ADR-006)"]
        PG[("PostgreSQL 16 + Flyway Migrations<br/>Tabelas: users, products, orders, order_items, navigation_audit")]
        PROM["Spring Actuator + Prometheus (/actuator/prometheus)<br/>Métricas de Latência p95, Pedidos ACID e Logs Estruturados"]
        K6["Grafana k6 + Runner de Carga (ADR-006)<br/>500 e 5.000 Usuários Simultâneos (Gate SLO p95 < 200ms)"]
    end

    CLIENTLIBS -.->|"Entrega Bundle JS/CSS"| VUE
    VUE <-->|"Envia automaticamente em cada request"| COOKIE
    VUE -->|"Queries/Mutations GraphQL ou REST"| CORR
    K6 -->|"Teste de Stress (500 e 5.000 VUs)"| CORR
    CORR --> JWT_FILTER
    JWT_FILTER --> GQL
    JWT_FILTER --> REST
    GQL -->|"Busca Produtos Transacionais"| PG
    GQL -->|"Resolve Conteúdo Editorial em Lote (@BatchMapping)"| JCR
    GQL --> ORDER_SVC
    REST --> ORDER_SVC
    ORDER_SVC -->|"Commit Transacional ACID"| PG
    CORR -->|"Exposição de Métricas e Logs"| PROM
```

---

## 2. Fluxo de Agregação GraphQL BFF + Solução do Problema de N+1 Queries (ADR-003)

O diagrama abaixo mostra como uma **única requisição GraphQL** do Vue.js traz dados combinados do **PostgreSQL** e do **AEM**, e como o `@BatchMapping` (**DataLoader**) evita que 6 produtos gerem 6 chamadas separadas ao AEM:

```mermaid
sequenceDiagram
    autonumber
    actor User as Usuário (Vue.js 3)
    participant GQL as GraphQlEcommerceController (/graphql)
    participant CatSvc as CatalogService (Java)
    participant PG as PostgreSQL (Dados Transacionais)
    participant Batch as @BatchMapping DataLoader
    participant AEM as AemContentService (JCR Content Fragments)

    User->>GQL: POST /graphql { storefrontCatalog { heroBanner products { sku price stock editorialContent { badge headline } } } }
    GQL->>CatSvc: getStorefrontCatalog()
    CatSvc->>PG: SELECT * FROM products (1 query SQL)
    PG-->>CatSvc: Retorna 6 produtos (SKU, Preço, Estoque ACID)
    CatSvc->>AEM: getStorefrontHeroBanner()
    AEM-->>CatSvc: Retorna Experience Fragment do Hero Banner
    CatSvc-->>GQL: Lista de 6 ProductView

    Note over GQL,Batch: Sem DataLoader seriam feitas +6 chamadas individuais (N+1).<br/>Com @BatchMapping, o Spring agrupa os 6 SKUs em 1 única chamada!
    GQL->>Batch: editorialContent(List<ProductView> com 6 produtos)
    Batch->>AEM: fetchEditorialByPathsBatch(Set<String> com 6 JCR paths)
    AEM-->>Batch: Retorna Map<Path, AemProductEditorial> de uma só vez
    Batch-->>GQL: Associa cada Content Fragment ao seu respectivo Produto
    GQL-->>User: JSON unificado (PostgreSQL + AEM JCR) em < 15ms!
```

---

## 3. Fluxo de Autenticação Stateless OAuth 2.0 + JWT em Cookie HttpOnly & Deny List (ADR-005)

```mermaid
sequenceDiagram
    autonumber
    actor Browser as Navegador (Vue.js)
    participant Auth as AuthController (/api/v1/auth)
    participant Filter as HttpOnlyCookieJwtFilter
    participant DenyList as TokenDenyListService (Cache/Memória)
    participant Jwt as JwtService (Assinatura HMAC-SHA256)
    participant PG as PostgreSQL (Tabela users)

    rect rgb(15, 35, 60)
        Note over Browser,PG: 1. Processo de Login (Consulta o banco apenas 1 vez)
        Browser->>Auth: POST /api/v1/auth/login (email + senha)
        Auth->>PG: findByEmailIgnoreCase(email)
        PG-->>Auth: UserEntity (hash BCrypt)
        Auth->>Auth: passwordEncoder.matches(senha, hash)
        Auth->>Jwt: generateToken(user) -> inclui jti, sub, role, planTier
        Auth-->>Browser: HTTP 200 + Header Set-Cookie: ACCESS_TOKEN=<jwt>; HttpOnly; SameSite=Lax
    end

    rect rgb(15, 50, 40)
        Note over Browser,PG: 2. Requisições Subsequentes ao GraphQL/REST (100% Stateless - Zero consulta ao Banco!)
        Browser->>Filter: POST /graphql (Cookie: ACCESS_TOKEN=<jwt> enviado automaticamente)
        Filter->>Jwt: parseAndValidateClaims(jwt) (Validação criptográfica em memória)
        Filter->>DenyList: isRevoked(jti)?
        DenyList-->>Filter: false (Token ativo)
        Filter->>Filter: Popula SecurityContextHolder com AuthenticatedUserPrincipal
        Filter-->>Browser: Executa Mutation createOrder autenticada!
    end

    rect rgb(60, 20, 30)
        Note over Browser,PG: 3. Logout Seguro com Revogação Imediata na Deny List
        Browser->>Auth: POST /api/v1/auth/logout
        Auth->>DenyList: revokeToken(jti, expirationInstant)
        Auth-->>Browser: Set-Cookie: ACCESS_TOKEN=""; Max-Age=0
    end
```

---

## 4. Pipeline de Build do Front-end Vue.js para AEM ClientLibs (ADR-004)

```mermaid
flowchart LR
    A["Código Fonte Vue 3<br/>(.vue, .ts, style.css)<br/>em ui.frontend/src"] --> B["Vite 5 Bundler<br/>(Transpilação TS + Minificação)"]
    B --> C["Arquivos Estáticos com Hash<br/>aem-vue-app.[hash].js<br/>aem-vue-style.[hash].css"]
    C --> D["Script Customizado<br/>aem-clientlib-generator.mjs"]
    D --> E["Pacote AEM ClientLibs em ui.apps/<br/>• .content.xml (cq:ClientLibraryFolder)<br/>• js.txt (#base=js)<br/>• css.txt (#base=css)"]
    E --> F["Deploy Containerizado / AEM Publish"]
```

