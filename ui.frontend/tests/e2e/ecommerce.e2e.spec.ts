import { chromium, type Browser, type Page } from '@playwright/test';
import { afterAll, beforeAll, describe, expect, it } from 'vitest';

const BASE_URL = process.env.E2E_BASE_URL || 'http://localhost:5173';

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

  it('deve carregar vitrine AEM+PostgreSQL via GraphQL, autenticar com Cookie HttpOnly e concluir pedido ACID', async () => {
    await page.goto(BASE_URL, { waitUntil: 'networkidle' });

    // 1. Verifica renderização do Experience Fragment do AEM e produtos do catálogo via GraphQL
    const heroTitle = await page.locator('.aem-hero__title').textContent();
    expect(heroTitle).toContain('Arquitetura Composable');

    const ultrabookCard = page.getByTestId('product-card-SKU-PRO-ULTRA-16');
    await ultrabookCard.waitFor({ state: 'visible' });
    expect(await ultrabookCard.textContent()).toContain('Destaque Editorial AEM');

    // 2. Adiciona produto ao carrinho
    await page.getByTestId('add-to-cart-SKU-KEY-MECH-PRO').click();
    expect(await page.getByTestId('open-cart-btn').textContent()).toContain('Carrinho (1)');

    // 3. Realiza Login OAuth2 + JWT via Cookie HttpOnly (ADR-005)
    await page.getByTestId('open-login-btn').click();
    await page.getByTestId('login-modal').waitFor({ state: 'visible' });
    await page.getByTestId('submit-login-btn').click();

    await page.getByTestId('authenticated-user').waitFor({ state: 'visible' });
    expect(await page.getByTestId('authenticated-user').textContent()).toContain('Alex Candidato');

    // Garante que NENHUM token JWT foi salvo no localStorage (ADR-005 anti-XSS)
    const localStorageKeys = await page.evaluate(() => Object.keys(window.localStorage));
    expect(localStorageKeys.length).toBe(0);

    // Verifica que o Cookie HttpOnly ACCESS_TOKEN está presente no contexto do navegador
    const cookies = await page.context().cookies();
    const accessTokenCookie = cookies.find((c) => c.name === 'ACCESS_TOKEN');
    expect(accessTokenCookie).toBeDefined();
    expect(accessTokenCookie?.httpOnly).toBe(true);

    // 4. Abre o carrinho e finaliza pedido ACID via GraphQL Mutation
    await page.getByTestId('open-cart-btn').click();
    await page.getByTestId('cart-drawer').waitFor({ state: 'visible' });
    await page.getByTestId('checkout-submit-btn').click();

    // 5. Verifica confirmação do pedido transacional ACID e painel de telemetria
    await page.getByTestId('orders-section').waitFor({ state: 'visible' });
    expect(await page.getByTestId('orders-section').textContent()).toContain('PAID_ACID_CONFIRMED');

    await page.getByTestId('telemetry-panel').waitFor({ state: 'visible' });
    expect(await page.getByTestId('telemetry-panel').textContent()).toContain('ADR-005');
  });
});
