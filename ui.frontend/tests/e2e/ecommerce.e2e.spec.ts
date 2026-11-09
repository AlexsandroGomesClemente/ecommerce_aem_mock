import { chromium, type Browser, type Page } from '@playwright/test';
import { afterAll, beforeAll, describe, expect, it } from 'vitest';

const BASE_URL = process.env.E2E_BASE_URL || 'http://localhost';

describe('Jornada E2E Real Browser (Playwright + Vue 3 + AEM + GraphQL + Cookie HttpOnly + PostgreSQL ACID)', () => {
  let browser: Browser;
  let page: Page;

  beforeAll(async () => {
    browser = await chromium.launch({
      channel: 'msedge',
      headless: true
    });
    const context = await browser.newContext();
    page = await context.newPage();
  });

  afterAll(async () => {
    if (browser) {
      await browser.close();
    }
  });

  it(
    'deve carregar vitrine AEM+PostgreSQL via GraphQL, autenticar com Cookie HttpOnly e navegar para telemetria',
    async () => {
    await page.goto(BASE_URL, { waitUntil: 'domcontentloaded' });

    // 1. Verifica renderização do Banner e do Catálogo com Fotos
    const heroTitle = page.locator('.aem-hero__title');
    await heroTitle.waitFor({ state: 'visible', timeout: 10000 });
    expect(await heroTitle.textContent()).toContain('Tecnologia de Alta Performance');

    const ultrabookCard = page.getByTestId('product-card-SKU-PRO-ULTRA-16');
    await ultrabookCard.waitFor({ state: 'visible', timeout: 10000 });
    expect(await ultrabookCard.textContent()).toContain('Destaque Editorial AEM');

    // 2. Adiciona produto ao carrinho
    await page.getByTestId('add-to-cart-SKU-KEY-MECH-PRO').click();
    expect(await page.getByTestId('open-cart-btn').textContent()).toContain('Carrinho (1)');

    // 3. Realiza Login
    await page.getByTestId('open-login-btn').click();
    await page.getByTestId('login-modal').waitFor({ state: 'visible' });
    await page.getByTestId('submit-login-btn').click();

    await page.getByTestId('authenticated-user').waitFor({ state: 'visible', timeout: 10000 });
    expect(await page.getByTestId('authenticated-user').textContent()).toContain('Alexsandro Clemente');

    // 4. Abre o carrinho e finaliza compra
    await page.getByTestId('open-cart-btn').click();
    await page.getByTestId('cart-drawer').waitFor({ state: 'visible' });
    await page.getByTestId('checkout-submit-btn').click();

    // 5. Verifica confirmação do pedido
    await page.getByTestId('orders-section').waitFor({ state: 'visible', timeout: 10000 });
    expect(await page.getByTestId('orders-section').textContent()).toContain('PAID_ACID_CONFIRMED');

    // 6. Navega para a aba de Telemetria separada
    await page.getByTestId('tab-telemetry').click();
    await page.getByTestId('telemetry-panel').waitFor({ state: 'visible', timeout: 10000 });
    expect(await page.getByTestId('telemetry-panel').textContent()).toContain('Painel de Telemetria');
  }, 45000);
});
