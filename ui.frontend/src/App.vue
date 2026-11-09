<script setup lang="ts">
import { onMounted, ref } from 'vue';
import AemHeroBanner from './components/AemHeroBanner.vue';
import ProductCatalogCard from './components/ProductCatalogCard.vue';
import TelemetryAndArchitecturePanel from './components/TelemetryAndArchitecturePanel.vue';
import { useEcommerceStore } from './stores/useEcommerceStore';

const store = useEcommerceStore();

const loginEmail = ref('alex@ecommerce.com');
const loginPassword = ref('Senha@123');
const paymentMethod = ref('PIX_INSTANTANEO');
const showAuthModal = ref(false);
const showCartDrawer = ref(false);

const categories = ['TODOS', 'Notebooks', 'Smartphones', 'Audio', 'Monitores', 'Wearables', 'Perifericos'];

onMounted(async () => {
  await store.restoreSession();
  await store.loadStorefront();
});

async function handleCategoryChange(cat: string) {
  store.selectedCategory = cat;
  await store.loadStorefront();
}

async function handleProtocolToggle(protocol: 'GRAPHQL' | 'REST_API') {
  store.consumptionProtocol = protocol;
  await store.loadStorefront();
}

async function handleLoginSubmit() {
  const ok = await store.login(loginEmail.value, loginPassword.value);
  if (ok) {
    showAuthModal.value = false;
  }
}

async function handleCheckout() {
  if (!store.user) {
    showAuthModal.value = true;
    store.setFeedback('info', 'Autentique-se via OAuth2 Cookie HttpOnly para concluir seu pedido ACID.');
    return;
  }
  await store.checkout(paymentMethod.value);
}

function formatBrl(value: number): string {
  return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

function scrollToCatalog() {
  document.getElementById('catalog-section')?.scrollIntoView({ behavior: 'smooth' });
}
</script>

<template>
  <div class="app-shell">
    <!-- Top Bar Enterprise -->
    <header class="topbar">
      <div class="topbar__brand">
        <span class="brand-logo">⚡ QuantumStore</span>
        <span class="brand-tag">Vue 3 + AEM ClientLibs + Java GraphQL + PostgreSQL</span>
      </div>

      <div class="topbar__controls">
        <!-- Seletor de Protocolo: GraphQL vs API REST -->
        <div class="protocol-switcher" role="group" aria-label="Modo de Consumo da API">
          <button
            type="button"
            :class="{ active: store.consumptionProtocol === 'GRAPHQL' }"
            data-testid="switch-graphql"
            @click="handleProtocolToggle('GRAPHQL')"
          >
            GraphQL BFF
          </button>
          <button
            type="button"
            :class="{ active: store.consumptionProtocol === 'REST_API' }"
            data-testid="switch-rest"
            @click="handleProtocolToggle('REST_API')"
          >
            REST API
          </button>
        </div>

        <!-- Status de Autenticação ADR-005 -->
        <div v-if="store.user" class="user-chip" data-testid="authenticated-user">
          <span class="user-chip__dot"></span>
          <span>{{ store.user.name }} ({{ store.user.planTier }})</span>
          <button type="button" class="btn-link" data-testid="logout-btn" @click="store.logout()">
            Sair (Revogar JWT)
          </button>
        </div>
        <button
          v-else
          type="button"
          class="btn-auth"
          data-testid="open-login-btn"
          @click="showAuthModal = true"
        >
          🔐 Entrar (Cookie HttpOnly)
        </button>

        <button
          type="button"
          class="btn-cart"
          data-testid="open-cart-btn"
          @click="showCartDrawer = !showCartDrawer"
        >
          🛒 Carrinho ({{ store.cartCount }})
        </button>
      </div>
    </header>

    <!-- Notificação / Feedback -->
    <div
      v-if="store.feedbackBanner"
      class="feedback-banner"
      :class="`feedback-${store.feedbackBanner.type}`"
      data-testid="feedback-banner"
    >
      <span>{{ store.feedbackBanner.text }}</span>
      <button type="button" @click="store.feedbackBanner = null">✕</button>
    </div>

    <main class="main-container">
      <!-- Banner Editorial AEM (Experience Fragment) -->
      <AemHeroBanner
        v-if="store.heroBanner"
        :banner="store.heroBanner"
        :protocol="store.consumptionProtocol"
        @explore="scrollToCatalog"
      />

      <!-- Filtros e Busca do Catálogo -->
      <section id="catalog-section" class="catalog-toolbar">
        <div class="category-tabs">
          <button
            v-for="cat in categories"
            :key="cat"
            type="button"
            class="category-tab"
            :class="{ active: store.selectedCategory === cat }"
            @click="handleCategoryChange(cat)"
          >
            {{ cat }}
          </button>
        </div>

        <div class="search-box">
          <input
            v-model="store.searchQuery"
            type="search"
            placeholder="Buscar por nome, SKU ou categoria..."
            data-testid="catalog-search-input"
            @keyup.enter="store.loadStorefront()"
          />
          <button type="button" class="btn-secondary" @click="store.loadStorefront()">
            Filtrar
          </button>
        </div>
      </section>

      <div class="datasource-caption">
        <span>Fonte de Dados: <strong>{{ store.dataSourceInfo }}</strong></span>
        <span>Segurança: <strong>{{ store.authMessage }}</strong></span>
      </div>

      <!-- Grid de Produtos Combinando AEM JCR + PostgreSQL -->
      <section class="products-grid" data-testid="products-grid">
        <ProductCatalogCard
          v-for="product in store.products"
          :key="product.sku"
          :product="product"
          @add-to-cart="store.addToCart"
        />
      </section>

      <!-- Histórico de Pedidos Transacionais ACID no PostgreSQL -->
      <section v-if="store.orders.length > 0" class="orders-section" data-testid="orders-section">
        <h2>Meus Pedidos Transacionais (PostgreSQL ACID)</h2>
        <div class="orders-list">
          <div v-for="order in store.orders" :key="order.orderCode" class="order-card">
            <div class="order-card__header">
              <strong>{{ order.orderCode }}</strong>
              <span class="order-status">{{ order.status }}</span>
              <span>{{ order.paymentMethod }}</span>
              <span>{{ order.createdAt }}</span>
              <strong>{{ formatBrl(order.totalAmount) }}</strong>
            </div>
            <div class="order-card__items">
              <span v-for="item in order.items" :key="item.productSku" class="order-item-chip">
                {{ item.quantity }}x {{ item.productName }} ({{ item.productSku }})
              </span>
            </div>
          </div>
        </div>
      </section>

      <!-- Painel de Telemetria e Observabilidade -->
      <TelemetryAndArchitecturePanel
        :telemetry="store.telemetry"
        @refresh="store.refreshTelemetry()"
      />
    </main>

    <!-- Modal de Login OAuth2 + JWT Cookie HttpOnly (ADR-005) -->
    <div v-if="showAuthModal" class="modal-backdrop" data-testid="login-modal">
      <div class="modal-card">
        <div class="modal-header">
          <h3>Autenticação Stateless OAuth 2.0 + Cookie HttpOnly (ADR-005)</h3>
          <button type="button" @click="showAuthModal = false">✕</button>
        </div>
        <p class="modal-desc">
          O token JWT assinado será entregue via Cookie <code>HttpOnly; SameSite=Lax</code>,
          protegendo a sessão contra ataques XSS (nenhum token é salvo no <code>localStorage</code>).
        </p>
        <form @submit.prevent="handleLoginSubmit">
          <label>
            E-mail Corporativo / Cliente
            <input v-model="loginEmail" type="email" required data-testid="login-email-input" />
          </label>
          <label>
            Senha
            <input v-model="loginPassword" type="password" required data-testid="login-password-input" />
          </label>
          <div class="demo-credentials">
            <span>Credenciais pré-configuradas: <code>alex@ecommerce.com</code> / <code>Senha@123</code></span>
          </div>
          <div class="modal-actions">
            <button type="button" class="btn-secondary" @click="showAuthModal = false">
              Cancelar
            </button>
            <button type="submit" class="btn-primary" data-testid="submit-login-btn">
              Autenticar via Cookie HttpOnly
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Drawer do Carrinho e Checkout ACID -->
    <aside v-if="showCartDrawer" class="cart-drawer" data-testid="cart-drawer">
      <div class="cart-drawer__header">
        <h3>Carrinho Transacional ({{ store.cartCount }} itens)</h3>
        <button type="button" @click="showCartDrawer = false">✕</button>
      </div>

      <div v-if="store.cart.length === 0" class="cart-empty">
        Seu carrinho está vazio. Adicione itens do catálogo para testar a transação ACID no PostgreSQL.
      </div>

      <div v-else class="cart-items">
        <div v-for="item in store.cart" :key="item.product.sku" class="cart-item">
          <div>
            <strong>{{ item.product.name }}</strong>
            <div class="cart-item__meta">
              <code>{{ item.product.sku }}</code> • {{ formatBrl(item.product.price) }}
            </div>
          </div>
          <div class="cart-item__qty">
            <button
              type="button"
              @click="store.updateCartQuantity(item.product.sku, item.quantity - 1)"
            >
              -
            </button>
            <span>{{ item.quantity }}</span>
            <button
              type="button"
              @click="store.updateCartQuantity(item.product.sku, item.quantity + 1)"
            >
              +
            </button>
          </div>
        </div>

        <div class="cart-summary">
          <label>
            Método de Pagamento:
            <select v-model="paymentMethod">
              <option value="PIX_INSTANTANEO">PIX Instantâneo (0% Taxa)</option>
              <option value="CARTAO_CREDITO_PRIME">Cartão Prime Black 12x</option>
              <option value="BOLETO_CORPORATIVO">Faturamento Corporativo</option>
            </select>
          </label>
          <div class="cart-total-row">
            <span>Total do Pedido:</span>
            <strong data-testid="cart-total-value">{{ formatBrl(store.cartTotal) }}</strong>
          </div>
          <button
            type="button"
            class="btn-primary btn-full"
            data-testid="checkout-submit-btn"
            @click="handleCheckout"
          >
            ✓ Finalizar Pedido ACID ({{ store.consumptionProtocol }})
          </button>
        </div>
      </div>
    </aside>
  </div>
</template>

