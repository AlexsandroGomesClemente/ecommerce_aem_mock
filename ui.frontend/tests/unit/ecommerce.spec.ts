import { mount } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, it } from 'vitest';
import AemHeroBanner from '../../src/components/AemHeroBanner.vue';
import ProductCatalogCard from '../../src/components/ProductCatalogCard.vue';
import { useEcommerceStore } from '../../src/stores/useEcommerceStore';
import type { ProductView } from '../../src/types/ecommerce';

const sampleProduct: ProductView = {
  id: 1,
  sku: 'SKU-PRO-ULTRA-16',
  name: 'Ultrabook Quantum Pro 16" M3 Max',
  category: 'Notebooks',
  price: 14999.9,
  stock: 15,
  rating: 4.9,
  aemContentPath: '/content/dam/ecommerce/cf/products/sku-pro-ultra-16',
  editorialContent: {
    jcrPath: '/content/dam/ecommerce/cf/products/sku-pro-ultra-16',
    headline: 'Performance Extrema para Engenharia & Criatividade',
    richDescription: 'Chassi em alumínio aeroespacial e 64GB de memória unificada.',
    badge: 'Destaque Editorial AEM',
    highlights: ['Chip M3 Max 16-Core', '64GB RAM Unificada'],
    imageGradient: 'linear-gradient(135deg, #dcede1 0%, #ffffff 100%)',
    imageIcon: 'laptop',
    imageUrl: 'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?auto=format&fit=crop&w=600&q=80',
    seoKeywords: 'ultrabook, aem'
  }
};

describe('Front-end Unit Tests (Vue 3 + Pinia + AEM Components)', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
  });

  it('deve renderizar o componente AemHeroBanner com o atributo data-cq-data-path do JCR', () => {
    const wrapper = mount(AemHeroBanner, {
      props: {
        banner: {
          jcrPath: '/content/experience-fragments/ecommerce/br/pt/site/hero-banner/master',
          eyebrow: 'NOVA COLEÇÃO TECH & DESIGN',
          title: 'Tecnologia de Alta Performance para o seu Dia a Dia',
          subtitle: 'Descubra produtos selecionados com entrega rápida.',
          ctaLabel: 'Explorar Produtos',
          themeAccent: '#72b789',
          authoredBy: 'aem-author@enterprise.com',
          lastPublished: '2026-11-09'
        },
        protocol: 'GRAPHQL'
      }
    });

    expect(wrapper.attributes('data-cq-data-path')).toBe(
      '/content/experience-fragments/ecommerce/br/pt/site/hero-banner/master'
    );
    expect(wrapper.text()).toContain('Tecnologia de Alta Performance');
    expect(wrapper.text()).toContain('Consumo ativo: GRAPHQL');
  });

  it('deve renderizar o ProductCatalogCard com imagem e dados e emitir evento de carrinho', async () => {
    const wrapper = mount(ProductCatalogCard, {
      props: { product: sampleProduct }
    });

    expect(wrapper.text()).toContain('Ultrabook Quantum Pro 16" M3 Max');
    expect(wrapper.text()).toContain('Destaque Editorial AEM');
    expect(wrapper.text()).toContain('15 em estoque');

    const img = wrapper.find('.product-card__image');
    expect(img.exists()).toBe(true);
    expect(img.attributes('src')).toBe(sampleProduct.editorialContent.imageUrl);

    await wrapper.find('[data-testid="add-to-cart-SKU-PRO-ULTRA-16"]').trigger('click');
    expect(wrapper.emitted('add-to-cart')).toBeTruthy();
    expect(wrapper.emitted('add-to-cart')?.[0]?.[0]).toEqual(sampleProduct);
  });

  it('deve gerenciar a adição, atualização de quantidade e cálculo de total no Pinia Store', () => {
    const store = useEcommerceStore();
    expect(store.cartCount).toBe(0);

    store.addToCart(sampleProduct);
    expect(store.cartCount).toBe(1);
    expect(store.cartTotal).toBeCloseTo(14999.9, 2);

    store.updateCartQuantity('SKU-PRO-ULTRA-16', 2);
    expect(store.cartCount).toBe(2);
    expect(store.cartTotal).toBeCloseTo(29999.8, 2);

    store.updateCartQuantity('SKU-PRO-ULTRA-16', 0);
    expect(store.cartCount).toBe(0);
  });
});
