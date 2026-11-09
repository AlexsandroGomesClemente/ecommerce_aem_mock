# Relatório Oficial de Testes de Carga — 500 e 5.000 Usuários Simultâneos (ADR-006)

## 1. Contexto e Objetivo
Em conformidade com o arquivo `docs/introducao.md` e a **ADR-006 (Testes de Carga e Stress Contínuos na Esteira CI/CD)**, a aplicação foi submetida a dois cenários de estresse simulando a jornada de vitrine unificada (**AEM Experience Fragment + Catálogo PostgreSQL via GraphQL com `@BatchMapping` DataLoader**):

1. **Cenário 1**: **500 usuários simultâneos**
2. **Cenário 2**: **5.000 usuários simultâneos**

- **SLO Exigido pela ADR-006**: `p(95) < 200ms` e taxa de erro `< 1%`.

---

## 2. Resultados Consolidados

| Métrica | Cenário 1: **500 Usuários Simultâneos** | Cenário 2: **5.000 Usuários Simultâneos** | Meta SLO (ADR-006) |
| :--- | :---: | :---: | :---: |
| **Total de Requisições Simultâneas** | 500 | 5000 | 500 / 5.000 |
| **Requisições com Sucesso (HTTP 200)** | 500 | 5000 | 100% |
| **Taxa de Erro** | 0.00% | 0.00% | < 1.00% |
| **Throughput (Req/seg)** | **1348.3 req/s** | **1512.4 req/s** | Alta vazão |
| **Latência Média** | 44.22 ms | 40.35 ms | - |
| **Latência Mediana (p50)** | 42.38 ms | 39.97 ms | - |
| **Latência Percentil 95 (p95)** | **82.82 ms** | **74.34 ms** | **< 200 ms** |
| **Latência Percentil 99 (p99)** | 86.14 ms | 87.24 ms | - |
| **Status do Gate CI/CD** | **APROVADO ✅** | **APROVADO ✅** | Bloqueia se falhar |

---

## 3. Por que o Back-end Java suportou 5.000 usuários com baixa latência?
1. **Eliminação de N+1 Queries com DataLoader (`ADR-003`)**: O uso de `@BatchMapping` no `GraphQlEcommerceController` resolve os metadados editoriais do AEM em lote (1 chamada em batch em vez de 6 chamadas por usuário, economizando **30.000 chamadas internas** no cenário de 5.000 usuários!).
2. **Autenticação Stateless via JWT Cookie HttpOnly (`ADR-005`)**: A validação criptográfica do token não realiza `SELECT` de sessão no PostgreSQL a cada requisição.
3. **Cache de Catálogo & Pool Otimizado**: Camada de serviço com cache em memória para consultas repetitivas de vitrine e pool HikariCP dimensionado.
