<script setup lang="ts">
import type { AemHeroBanner } from '../types/ecommerce';

defineProps<{
  banner: AemHeroBanner;
  protocol: 'GRAPHQL' | 'REST_API';
}>();

defineEmits<{
  (e: 'explore'): void;
}>();
</script>

<template>
  <section
    class="aem-hero"
    :style="{ background: banner.themeAccent }"
    :data-cq-data-path="banner.jcrPath"
  >
    <div class="aem-hero__meta">
      <span class="aem-badge">{{ banner.eyebrow }}</span>
      <span class="aem-protocol">Consumo ativo: {{ protocol }}</span>
    </div>

    <h1 class="aem-hero__title">{{ banner.title }}</h1>
    <p class="aem-hero__subtitle">{{ banner.subtitle }}</p>

    <div class="aem-hero__actions">
      <button type="button" class="btn-primary" @click="$emit('explore')">
        {{ banner.ctaLabel }}
      </button>
    </div>
  </section>
</template>
