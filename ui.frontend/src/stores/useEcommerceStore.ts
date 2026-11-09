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

export interface ToastMessage {
  id: string;
  type: 'success' | 'error' | 'info';
  title: string;
  message: string;
}

export const useEcommerceStore = defineStore('ecommerce', () => {
  const heroBanner = ref<AemHeroBanner | null>(null);
  const products = ref<ProductView[]>([]);
  const dataSourceInfo = ref<string>('PostgreSQL 16 + AEM JCR Content Fragments (via GraphQL BFF)');
  const selectedCategory = ref<string>('TODOS');
  const searchQuery = ref<string>('');
  const consumptionProtocol = ref<'GRAPHQL' | 'REST_API'>('GRAPHQL');

  const cart = ref<CartItem[]>([]);
  const user = ref<UserProfileView | null>(null);
  const authMessage = ref<string>('Sessão anônima');
  const cookiePolicy = ref<string>('HttpOnly; Secure; SameSite=Lax');

  const orders = ref<OrderView[]>([]);
  const telemetry = ref<TelemetrySnapshot | null>(null);
  const loading = ref<boolean>(false);

  // Sistema de Toasts Notificações Flutuantes
  const toasts = ref<ToastMessage[]>([]);

  const cartCount = computed(() =>
    cart.value.reduce((acc, item) => acc + item.quantity, 0)
  );

  const cartTotal = computed(() =>
    cart.value.reduce((acc, item) => acc + item.product.price * item.quantity, 0)
  );

  function addToast(type: 'success' | 'error' | 'info', title: string, message: string, durationMs = 3800) {
    const id = Math.random().toString(36).substring(2, 9);
    toasts.value.push({ id, type, title, message });

    setTimeout(() => {
      removeToast(id);
    }, durationMs);
  }

  function removeToast(id: string) {
    const idx = toasts.value.findIndex((t) => t.id === id);
    if (idx !== -1) {
      toasts.value.splice(idx, 1);
    }
  }

  function setFeedback(type: 'success' | 'error' | 'info', message: string) {
    const title = type === 'success' ? 'Sucesso' : type === 'error' ? 'Atenção' : 'Informação';
    addToast(type, title, message);
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
      addToast('error', 'Erro ao carregar', err?.message || 'Falha ao buscar produtos da vitrine.');
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
        addToast('success', 'Login Realizado', `Bem-vindo de volta, ${res.user?.name || 'Cliente'}!`);
        await loadOrders();
        await refreshTelemetry();
        return true;
      } else {
        addToast('error', 'Falha no Login', res.message);
        return false;
      }
    } catch (err: any) {
      addToast('error', 'Falha de Autenticação', err?.message || 'Erro de conexão.');
      return false;
    }
  }

  async function logout() {
    const res = await ecommerceClient.logoutAndRevokeCookie();
    user.value = null;
    authMessage.value = res.message;
    cookiePolicy.value = res.cookiePolicy;
    orders.value = [];
    addToast('info', 'Sessão Encerrada', 'Você saiu da sua conta com sucesso.');
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
        addToast('success', 'Carrinho Atualizado', `+1 unidade de "${product.name}" adicionada.`);
      } else {
        addToast('error', 'Estoque Limite', `Não há mais unidades disponíveis de "${product.name}".`);
      }
    } else {
      cart.value.push({ product, quantity: 1 });
      addToast('success', 'Produto Adicionado', `"${product.name}" foi colocado no seu carrinho.`);
    }
  }

  function updateCartQuantity(sku: string, quantity: number) {
    const idx = cart.value.findIndex((item) => item.product.sku === sku);
    if (idx === -1) return;
    const productName = cart.value[idx].product.name;
    if (quantity <= 0) {
      cart.value.splice(idx, 1);
      addToast('info', 'Item Removido', `"${productName}" foi removido do carrinho.`);
    } else {
      cart.value[idx].quantity = quantity;
    }
  }

  function clearCart() {
    cart.value = [];
    addToast('info', 'Carrinho Limpo', 'Todos os itens foram esvaziados.');
  }

  async function checkout(paymentMethod = 'PIX_INSTANTANEO'): Promise<OrderView | null> {
    if (cart.value.length === 0) {
      addToast('error', 'Carrinho Vazio', 'Adicione produtos antes de finalizar a compra.');
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
      addToast(
        'success',
        'Pedido Confirmado!',
        `Pedido #${createdOrder.orderCode} gerado com sucesso. Pagamento via ${paymentMethod}.`
      );
      await loadStorefront();
      await loadOrders();
      return createdOrder;
    } catch (err: any) {
      addToast('error', 'Não foi possível finalizar', err?.message || 'Faça login para concluir o pedido.');
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
      // Ignora se backend estiver indisponível
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
    toasts,
    addToast,
    removeToast,
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
