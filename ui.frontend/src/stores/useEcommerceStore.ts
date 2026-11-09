import { defineStore } from 'pinia';
import { computed, ref } from 'vue';
import { ecommerceClient } from '../api/ecommerceClient';
import type {
  AemHeroBanner,
  CartItem,
  OrderView,
  ProductView,
  TelemetrySnapshot,
  UserProfileView
} from '../types/ecommerce';

export const useEcommerceStore = defineStore('ecommerce', () => {
  const heroBanner = ref<AemHeroBanner | null>(null);
  const products = ref<ProductView[]>([]);
  const dataSourceInfo = ref<string>('PostgreSQL 16 + AEM JCR Content Fragments (via GraphQL BFF)');
  const selectedCategory = ref<string>('TODOS');
  const searchQuery = ref<string>('');
  const consumptionProtocol = ref<'GRAPHQL' | 'REST_API'>('GRAPHQL');

  const cart = ref<CartItem[]>([]);
  const user = ref<UserProfileView | null>(null);
  const authMessage = ref<string>('Sessão anônima (Nenhum JWT no localStorage - ADR-005)');
  const cookiePolicy = ref<string>('HttpOnly; Secure; SameSite=Lax');

  const orders = ref<OrderView[]>([]);
  const telemetry = ref<TelemetrySnapshot | null>(null);
  const loading = ref<boolean>(false);
  const feedbackBanner = ref<{ type: 'success' | 'error' | 'info'; text: string } | null>(null);

  const cartCount = computed(() =>
    cart.value.reduce((acc, item) => acc + item.quantity, 0)
  );

  const cartTotal = computed(() =>
    cart.value.reduce((acc, item) => acc + item.product.price * item.quantity, 0)
  );

  function setFeedback(type: 'success' | 'error' | 'info', text: string) {
    feedbackBanner.value = { type, text };
  }

  async function loadStorefront() {
    loading.value = true;
    try {
      const response =
        consumptionProtocol.value === 'GRAPHQL'
          ? await ecommerceClient.fetchStorefrontViaGraphQl(selectedCategory.value, searchQuery.value)
          : await ecommerceClient.fetchStorefrontViaRest(selectedCategory.value, searchQuery.value);

      heroBanner.value = response.heroBanner;
      products.value = response.products;
      dataSourceInfo.value = response.dataSourceInfo;
      await refreshTelemetry();
    } catch (err: any) {
      setFeedback('error', err?.message || 'Erro ao carregar vitrine do e-commerce.');
    } finally {
      loading.value = false;
    }
  }

  async function login(email: string, password: string): Promise<boolean> {
    try {
      const res = await ecommerceClient.loginWithHttpOnlyCookie(email, password);
      user.value = res.user;
      authMessage.value = res.message;
      cookiePolicy.value = res.cookiePolicy;

      if (res.authenticated) {
        setFeedback('success', `Bem-vindo, ${res.user?.name}! Autenticado via Cookie HttpOnly (ADR-005).`);
        await loadOrders();
        await refreshTelemetry();
        return true;
      } else {
        setFeedback('error', res.message);
        return false;
      }
    } catch (err: any) {
      setFeedback('error', err?.message || 'Erro ao autenticar.');
      return false;
    }
  }

  async function logout() {
    const res = await ecommerceClient.logoutAndRevokeCookie();
    user.value = null;
    authMessage.value = res.message;
    cookiePolicy.value = res.cookiePolicy;
    orders.value = [];
    setFeedback('info', res.message);
    await refreshTelemetry();
  }

  async function restoreSession() {
    try {
      const res = await ecommerceClient.checkSession();
      if (res.authenticated && res.user) {
        user.value = res.user;
        authMessage.value = res.message;
        cookiePolicy.value = res.cookiePolicy;
        await loadOrders();
      }
    } catch {
      // Ignora erro de rede inicial
    }
  }

  function addToCart(product: ProductView) {
    const existing = cart.value.find((item) => item.product.sku === product.sku);
    if (existing) {
      if (existing.quantity < product.stock) {
        existing.quantity += 1;
      }
    } else {
      cart.value.push({ product, quantity: 1 });
    }
    setFeedback('info', `${product.name} adicionado ao carrinho.`);
  }

  function updateCartQuantity(sku: string, quantity: number) {
    const idx = cart.value.findIndex((item) => item.product.sku === sku);
    if (idx === -1) return;
    if (quantity <= 0) {
      cart.value.splice(idx, 1);
    } else {
      cart.value[idx].quantity = quantity;
    }
  }

  function clearCart() {
    cart.value = [];
  }

  async function checkout(paymentMethod = 'PIX_INSTANTANEO'): Promise<OrderView | null> {
    if (cart.value.length === 0) {
      setFeedback('error', 'Adicione pelo menos 1 produto ao carrinho antes de finalizar.');
      return null;
    }

    const itemsPayload = cart.value.map((item) => ({
      sku: item.product.sku,
      quantity: item.quantity
    }));

    try {
      const createdOrder =
        consumptionProtocol.value === 'GRAPHQL'
          ? await ecommerceClient.createOrderViaGraphQl(paymentMethod, itemsPayload)
          : await ecommerceClient.createOrderViaRest(paymentMethod, itemsPayload);

      clearCart();
      setFeedback(
        'success',
        `Pedido ${createdOrder.orderCode} confirmado com transação ACID no PostgreSQL!`
      );
      await loadStorefront();
      await loadOrders();
      return createdOrder;
    } catch (err: any) {
      setFeedback('error', err?.message || 'Faça login (Cookie HttpOnly) para concluir o checkout.');
      return null;
    }
  }

  async function loadOrders() {
    try {
      orders.value = await ecommerceClient.fetchMyOrdersViaGraphQl();
    } catch {
      orders.value = [];
    }
  }

  async function refreshTelemetry() {
    try {
      telemetry.value = await ecommerceClient.fetchTelemetryViaGraphQl();
    } catch {
      // Ignora se backend estiver indisponível no teste unitário isolado
    }
  }

  return {
    heroBanner,
    products,
    dataSourceInfo,
    selectedCategory,
    searchQuery,
    consumptionProtocol,
    cart,
    cartCount,
    cartTotal,
    user,
    authMessage,
    cookiePolicy,
    orders,
    telemetry,
    loading,
    feedbackBanner,
    setFeedback,
    loadStorefront,
    login,
    logout,
    restoreSession,
    addToCart,
    updateCartQuantity,
    clearCart,
    checkout,
    loadOrders,
    refreshTelemetry
  };
});

