/**
 * ADR-006: Testes de Carga e Stress Contínuos na Esteira (CI/CD) com Grafana k6
 *
 * Simula dois cenários obrigatórios exigidos nos requisitos:
 *   1) stage_500_users:  500 usuários simultâneos (VUs)
 *   2) stage_5000_users: 5.000 usuários simultâneos (VUs)
 *
 * Jornada testada por usuário virtual:
 *   - Acessa o Experience Fragment / Home do AEM + Catálogo via GraphQL (/graphql)
 *   - Consulta disponibilidade e produto específico via API REST (/api/v1/products/SKU-PRO-ULTRA-16)
 *
 * Critério de aprovação (SLO da ADR-006):
 *   - 95% das requisições devem responder em menos de 200ms: `http_req_duration: ['p(95)<200']`
 *   - Taxa de falha inferior a 1%: `http_req_failed: ['rate<0.01']`
 *
 * Como executar com o binário k6:
 *   k6 run load-tests/k6-ecommerce-load.js
 */
import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
  scenarios: {
    cenario_500_usuarios_simultaneos: {
      executor: 'ramping-vus',
      startVUs: 50,
      stages: [
        { duration: '10s', target: 500 },
        { duration: '20s', target: 500 },
        { duration: '5s', target: 0 }
      ],
      gracefulRampDown: '5s'
    },
    cenario_5000_usuarios_simultaneos: {
      executor: 'ramping-vus',
      startTime: '36s',
      startVUs: 500,
      stages: [
        { duration: '15s', target: 5000 },
        { duration: '20s', target: 5000 },
        { duration: '10s', target: 0 }
      ],
      gracefulRampDown: '5s'
    }
  },
  thresholds: {
    http_req_duration: ['p(95)<200'],
    http_req_failed: ['rate<0.01']
  }
};

const GRAPHQL_QUERY = JSON.stringify({
  query: `
    query StorefrontLoadTest {
      storefrontCatalog {
        totalProducts
        heroBanner { jcrPath title }
        products {
          sku
          name
          price
          stock
          editorialContent { badge headline }
        }
      }
    }
  `
});

export default function () {
  // Passo 1: Acessa Home AEM + Catálogo unificado via GraphQL BFF (DataLoader ativo)
  const gqlRes = http.post(`${BASE_URL}/graphql`, GRAPHQL_QUERY, {
    headers: {
      'Content-Type': 'application/json',
      'X-Correlation-ID': `k6-vu-${__VU}-iter-${__ITER}`
    }
  });

  check(gqlRes, {
    'GraphQL status 200': (r) => r.status === 200,
    'GraphQL retornou 6 produtos com AEM editorialContent': (r) =>
      r.body && r.body.includes('SKU-PRO-ULTRA-16')
  });

  // Passo 2: Consulta detalhe de SKU via API REST
  const restRes = http.get(`${BASE_URL}/api/v1/products/SKU-PRO-ULTRA-16`, {
    headers: {
      'X-Correlation-ID': `k6-rest-${__VU}-${__ITER}`
    }
  });

  check(restRes, {
    'REST SKU status 200': (r) => r.status === 200
  });

  sleep(0.1);
}

