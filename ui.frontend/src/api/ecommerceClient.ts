import type {
  AuthStatusResponse,
  OrderView,
  StorefrontCatalogResponse,
  TelemetrySnapshot
} from '../types/ecommerce';

const API_BASE = import.meta.env.VITE_API_BASE_URL || '';

function generateCorrelationId(prefix = 'vue'): string {
  const randomPart = Math.random().toString(36).substring(2, 10);
  return `${prefix}-${randomPart}`;
}

/**
 * ADR-003 & ADR-005: Cliente unificado GraphQL + REST API.
 * Utiliza `credentials: 'include'` em todas as chamadas para trafegar o Cookie HttpOnly
 * de forma transparente e segura contra ataques XSS (nenhum token é salvo no localStorage).
 */
export const ecommerceClient = {
  async queryGraphQl<T>(query: string, variables: Record<string, unknown> = {}): Promise<T> {
    const correlationId = generateCorrelationId('gql');
    const response = await fetch(`${API_BASE}/graphql`, {
      method: 'POST',
      credentials: 'include',
      headers: {
        'Content-Type': 'application/json',
        'X-Correlation-ID': correlationId
      },
      body: JSON.stringify({ query, variables })
    });

    const payload = await response.json();
    if (payload.errors && payload.errors.length > 0) {
      throw new Error(payload.errors[0].message || 'Erro ao executar operação GraphQL');
    }
    return payload.data as T;
  },

  async fetchStorefrontViaGraphQl(category?: string, search?: string): Promise<StorefrontCatalogResponse> {
    const query = `
      query StorefrontCatalog($category: String, $search: String) {
        storefrontCatalog(category: $category, search: $search) {
          totalProducts
          dataSourceInfo
          heroBanner {
            jcrPath
            eyebrow
            title
            subtitle
            ctaLabel
            themeAccent
            authoredBy
            lastPublished
          }
          products {
            id
            sku
            name
            category
            price
            stock
            rating
            aemContentPath
            editorialContent {
              jcrPath
              headline
              richDescription
              badge
              highlights
              imageGradient
              imageIcon
              imageUrl
              seoKeywords
            }
          }
        }
      }
    `;
    const data = await this.queryGraphQl<{ storefrontCatalog: StorefrontCatalogResponse }>(query, {
      category: category && category !== 'TODOS' ? category : null,
      search: search || null
    });
    return data.storefrontCatalog;
  },

  async fetchStorefrontViaRest(category?: string, search?: string): Promise<StorefrontCatalogResponse> {
    const params = new URLSearchParams();
    if (category && category !== 'TODOS') params.set('category', category);
    if (search) params.set('search', search);
    const qs = params.toString() ? `?${params.toString()}` : '';

    const response = await fetch(`${API_BASE}/api/v1/products${qs}`, {
      method: 'GET',
      credentials: 'include',
      headers: {
        'X-Correlation-ID': generateCorrelationId('rest')
      }
    });
    if (!response.ok) {
      throw new Error('Falha ao consultar catálogo via API REST');
    }
    return response.json();
  },

  async loginWithHttpOnlyCookie(email: string, password: string): Promise<AuthStatusResponse> {
    const response = await fetch(`${API_BASE}/api/v1/auth/login`, {
      method: 'POST',
      credentials: 'include',
      headers: {
        'Content-Type': 'application/json',
        'X-Correlation-ID': generateCorrelationId('auth')
      },
      body: JSON.stringify({ email, password })
    });
    return response.json();
  },

  async logoutAndRevokeCookie(): Promise<AuthStatusResponse> {
    const response = await fetch(`${API_BASE}/api/v1/auth/logout`, {
      method: 'POST',
      credentials: 'include',
      headers: {
        'X-Correlation-ID': generateCorrelationId('logout')
      }
    });
    return response.json();
  },

  async checkSession(): Promise<AuthStatusResponse> {
    const response = await fetch(`${API_BASE}/api/v1/auth/me`, {
      method: 'GET',
      credentials: 'include',
      headers: {
        'X-Correlation-ID': generateCorrelationId('session')
      }
    });
    return response.json();
  },

  async createOrderViaGraphQl(
    paymentMethod: string,
    items: Array<{ sku: string; quantity: number }>
  ): Promise<OrderView> {
    const mutation = `
      mutation CreateOrder($input: CreateOrderInput!) {
        createOrder(input: $input) {
          id
          orderCode
          userEmail
          totalAmount
          status
          paymentMethod
          createdAt
          items {
            productSku
            productName
            quantity
            unitPrice
          }
        }
      }
    `;
    const data = await this.queryGraphQl<{ createOrder: OrderView }>(mutation, {
      input: { paymentMethod, items }
    });
    return data.createOrder;
  },

  async createOrderViaRest(
    paymentMethod: string,
    items: Array<{ sku: string; quantity: number }>
  ): Promise<OrderView> {
    const response = await fetch(`${API_BASE}/api/v1/orders`, {
      method: 'POST',
      credentials: 'include',
      headers: {
        'Content-Type': 'application/json',
        'X-Correlation-ID': generateCorrelationId('order-rest')
      },
      body: JSON.stringify({ paymentMethod, items })
    });
    if (!response.ok) {
      const err = await response.json().catch(() => ({ message: 'Erro ao criar pedido via REST' }));
      throw new Error(err.message || 'Não autorizado. Faça login primeiro.');
    }
    return response.json();
  },

  async fetchMyOrdersViaGraphQl(): Promise<OrderView[]> {
    const query = `
      query MyOrders {
        myOrders {
          id
          orderCode
          userEmail
          totalAmount
          status
          paymentMethod
          createdAt
          items {
            productSku
            productName
            quantity
            unitPrice
          }
        }
      }
    `;
    const data = await this.queryGraphQl<{ myOrders: OrderView[] }>(query);
    return data.myOrders;
  },

  async fetchTelemetryViaGraphQl(): Promise<TelemetrySnapshot> {
    const query = `
      query Telemetry {
        telemetrySnapshot {
          totalRequests
          graphqlRequests
          restRequests
          ordersCreated
          avgLatencyMs
          p95LatencyMs
          activeDenyListTokens
          recentLogs {
            timestamp
            correlationId
            level
            component
            message
            latencyMs
          }
        }
      }
    `;
    const data = await this.queryGraphQl<{ telemetrySnapshot: TelemetrySnapshot }>(query);
    return data.telemetrySnapshot;
  }
};
