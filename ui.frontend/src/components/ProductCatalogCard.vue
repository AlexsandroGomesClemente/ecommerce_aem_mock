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
    <div class="product-card__visual">
      <img
        :src="product.editorialContent?.imageUrl"
        :alt="product.name"
        class="product-card__image"
        loading="lazy"
        @error="(e) => ((e.target as HTMLImageElement).src = 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=600&q=80')"
      />
      <span class="product-card__badge">
        {{ product.editorialContent?.badge || 'Destaque' }}
      </span>
      <span class="product-card__category">{{ product.category }}</span>
    </div>

    <div class="product-card__body">
      <div class="product-card__header-info">
        <span class="product-card__rating">★ {{ product.rating.toFixed(1) }}</span>
        <code class="product-card__sku">{{ product.sku }}</code>
      </div>

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
    </div>

    <div class="product-card__footer">
      <div class="product-card__pricing">
        <span class="price-label">Preço à vista</span>
        <strong class="price-value">{{ formatBrl(product.price) }}</strong>
        <span class="stock-pill" :class="{ 'stock-low': product.stock < 50 }">
          {{ product.stock > 0 ? `${product.stock} em estoque` : 'Sem estoque' }}
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
