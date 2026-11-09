<script setup lang="ts">
import type { ProductView } from '../types/ecommerce';

defineProps<{
  product: ProductView;
}>();

defineEmits<{
  (e: 'add-to-cart', product: ProductView): void;
}>();

function formatBrl(value: number): string {
  return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}
</script>

<template>
  <article
    class="product-card"
    :data-cq-data-path="product.aemContentPath"
    :data-testid="`product-card-${product.sku}`"
  >
    <div
      class="product-card__visual"
      :style="{ background: product.editorialContent?.imageGradient || '#1e293b' }"
    >
      <span class="product-card__badge">
        {{ product.editorialContent?.badge || 'AEM Content Fragment' }}
      </span>
      <span class="product-card__category">{{ product.category }}</span>
      <div class="product-card__sku-row">
        <code>{{ product.sku }}</code>
        <span>★ {{ product.rating.toFixed(1) }}</span>
      </div>
    </div>

    <div class="product-card__body">
      <h3 class="product-card__title">{{ product.name }}</h3>
      <p class="product-card__headline">
        {{ product.editorialContent?.headline }}
      </p>
      <p class="product-card__description">
        {{ product.editorialContent?.richDescription }}
      </p>

      <ul v-if="product.editorialContent?.highlights?.length" class="product-card__highlights">
        <li v-for="item in product.editorialContent.highlights" :key="item">
          ✓ {{ item }}
        </li>
      </ul>

      <div class="product-card__jcr-tag">
        <span>AEM JCR:</span>
        <code>{{ product.aemContentPath }}</code>
      </div>
    </div>

    <div class="product-card__footer">
      <div class="product-card__pricing">
        <span class="price-label">Preço PostgreSQL (ACID)</span>
        <strong class="price-value">{{ formatBrl(product.price) }}</strong>
        <span class="stock-pill" :class="{ 'stock-low': product.stock < 50 }">
          Estoque: {{ product.stock }} unid.
        </span>
      </div>

      <button
        type="button"
        class="btn-add-cart"
        :data-testid="`add-to-cart-${product.sku}`"
        :disabled="product.stock <= 0"
        @click="$emit('add-to-cart', product)"
      >
        {{ product.stock > 0 ? '+ Adicionar' : 'Esgotado' }}
      </button>
    </div>
  </article>
</template>

