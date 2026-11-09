<script setup lang="ts">
import { onMounted, ref } from 'vue';
import AemHeroBanner from './components/AemHeroBanner.vue';
import ProductCatalogCard from './components/ProductCatalogCard.vue';
import TelemetryAndArchitecturePanel from './components/TelemetryAndArchitecturePanel.vue';
import { useEcommerceStore } from './stores/useEcommerceStore';

const store = useEcommerceStore();

const activeTab = ref<'STORE' | 'TELEMETRY'>('STORE');
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
  store.addToast('info', 'Filtro Aplicado', `Categoria alterada para: ${cat}`);
}

async function handleProtocolToggle(protocol: 'GRAPHQL' | 'REST_API') {
  store.consumptionProtocol = protocol;
  await store.loadStorefront();
  store.addToast('info', 'Protocolo Alterado', `Consumo ativo alternado para: ${protocol}`);
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
    store.addToast('info', 'Login Necessário', 'Acesse sua conta para concluir a compra.');
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
    <!-- Top Bar -->
    <header class="topbar">
      <div class="topbar__brand">
        <span class="brand-logo">⚡ QuantumStore</span>
      </div>

      <!-- Navegação Principal entre Loja e Telemetria -->
      <nav class="main-nav">
        <button
          type="button"
          class="nav-tab-btn"
          :class="{ active: activeTab === 'STORE' }"
          data-testid="tab-store"
          @click="activeTab = 'STORE'"
        >
          Loja
        </button>
        <button
          type="button"
          class="nav-tab-btn"
          :class="{ active: activeTab === 'TELEMETRY' }"
          data-testid="tab-telemetry"
          @click="activeTab = 'TELEMETRY'; store.refreshTelemetry()"
        >
          Telemetria
        </button>
      </nav>

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

        <!-- Status de Autenticação -->
        <div v-if="store.user" class="user-chip" data-testid="authenticated-user">
          <span class="user-chip__dot"></span>
          <span>{{ store.user.name }}</span>
          <button type="button" class="btn-link" data-testid="logout-btn" @click="store.logout()">
            Sair
          </button>
        </div>
        <button
          v-else
          type="button"
          class="btn-auth"
          data-testid="open-login-btn"
          @click="showAuthModal = true"
        >
          Entrar
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

    <!-- Sistema Flutuante de Toasts Notificações -->
    <div class="toast-container" aria-live="polite" data-testid="toast-container">
      <TransitionGroup name="toast">
        <div
          v-for="toast in store.toasts"
          :key="toast.id"
          class="toast-card"
          :class="`toast-${toast.type}`"
          data-testid="toast-item"
        >
          <div class="toast-icon">
            <span v-if="toast.type === 'success'">✓</span>
            <span v-else-if="toast.type === 'error'">✕</span>
            <span v-else>ℹ</span>
          </div>
          <div class="toast-content">
            <strong class="toast-title">{{ toast.title }}</strong>
            <p class="toast-message">{{ toast.message }}</p>
          </div>
          <button type="button" class="toast-close" @click="store.removeToast(toast.id)">✕</button>
        </div>
      </TransitionGroup>
    </div>

    <!-- TELA 1: LOJA / VITRINE -->
    <main v-if="activeTab === 'STORE'" class="main-container">
      <!-- Banner Editorial AEM -->
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
            placeholder="Buscar produto por nome ou categoria..."
            data-testid="catalog-search-input"
            @keyup.enter="store.loadStorefront()"
          />
          <button type="button" class="btn-secondary" @click="store.loadStorefront()">
            Buscar
          </button>
        </div>
      </section>

      <!-- Grid de Produtos com Fotos -->
      <section class="products-grid" data-testid="products-grid">
        <ProductCatalogCard
          v-for="product in store.products"
          :key="product.sku"
          :product="product"
          @add-to-cart="store.addToCart"
        />
      </section>

      <!-- Histórico de Pedidos -->
      <section v-if="store.orders.length > 0" class="orders-section" data-testid="orders-section">
        <h2>Meus Pedidos</h2>
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
                {{ item.quantity }}x {{ item.productName }}
              </span>
            </div>
          </div>
        </div>
      </section>
    </main>

    <!-- TELA 2: TELEMETRIA SEPARADA -->
    <main v-else-if="activeTab === 'TELEMETRY'" class="main-container">
      <TelemetryAndArchitecturePanel
        :telemetry="store.telemetry"
        @refresh="store.refreshTelemetry()"
      />
    </main>

    <!-- Modal de Login -->
    <div v-if="showAuthModal" class="modal-backdrop" data-testid="login-modal">
      <div class="modal-card">
        <div class="modal-header">
          <h3>Acessar sua Conta</h3>
          <button type="button" class="close-btn" @click="showAuthModal = false">✕</button>
        </div>
        <p class="modal-desc">
          Informe seu e-mail e senha para acessar seus pedidos e concluir compras com segurança.
        </p>
        <form @submit.prevent="handleLoginSubmit">
          <label>
            E-mail
            <input v-model="loginEmail" type="email" placeholder="seu@email.com" required data-testid="login-email-input" />
          </label>
          <label>
            Senha
            <input v-model="loginPassword" type="password" placeholder="Digite sua senha" required data-testid="login-password-input" />
          </label>
          <div class="demo-credentials">
            <span>Credenciais de teste: <code>alex@ecommerce.com</code> / <code>Senha@123</code></span>
          </div>
          <div class="modal-actions">
            <button type="button" class="btn-secondary" @click="showAuthModal = false">
              Cancelar
            </button>
            <button type="submit" class="btn-primary" data-testid="submit-login-btn">
              Entrar
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Drawer do Carrinho -->
    <aside v-if="showCartDrawer" class="cart-drawer" data-testid="cart-drawer">
      <div class="cart-drawer__header">
        <h3>Seu Carrinho ({{ store.cartCount }} {{ store.cartCount === 1 ? 'item' : 'itens' }})</h3>
        <button type="button" class="close-btn" @click="showCartDrawer = false">✕</button>
      </div>

      <div v-if="store.cart.length === 0" class="cart-empty">
        Seu carrinho está vazio no momento. Adicione itens para finalizar a compra.
      </div>

      <div v-else class="cart-items">
        <div v-for="item in store.cart" :key="item.product.sku" class="cart-item">
          <div class="cart-item__info">
            <strong>{{ item.product.name }}</strong>
            <div class="cart-item__meta">
              {{ formatBrl(item.product.price) }}
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
            Forma de Pagamento:
            <select v-model="paymentMethod">
              <option value="PIX_INSTANTANEO">PIX Instantâneo</option>
              <option value="CARTAO_CREDITO_PRIME">Cartão de Crédito</option>
              <option value="BOLETO_CORPORATIVO">Boleto Bancário</option>
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
            Finalizar Compra
          </button>
        </div>
      </div>
    </aside>
  </div>
</template>
