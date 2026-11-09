package com.ecommerce;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class EcommerceBffIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Deve agregar dados do PostgreSQL e do AEM em uma única chamada GraphQL com DataLoader")
    void shouldExecuteGraphQlStorefrontQueryCombiningPostgresAndAem() throws Exception {
        String graphqlPayload = """
                {
                  "query": "query { storefrontCatalog { totalProducts heroBanner { title jcrPath } products { sku name price stock editorialContent { badge headline highlights } } } }"
                }
                """;

        mockMvc.perform(post("/graphql")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(graphqlPayload))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-ID"))
                .andExpect(jsonPath("$.data.storefrontCatalog.totalProducts").value(6))
                .andExpect(jsonPath("$.data.storefrontCatalog.heroBanner.jcrPath")
                        .value("/content/experience-fragments/ecommerce/br/pt/site/hero-banner/master"))
                .andExpect(jsonPath("$.data.storefrontCatalog.products[0].sku").value("SKU-PRO-ULTRA-16"))
                .andExpect(jsonPath("$.data.storefrontCatalog.products[0].editorialContent.badge")
                        .value("Destaque Editorial AEM"));
    }

    @Test
    @DisplayName("Deve autenticar via Cookie HttpOnly, criar pedido ACID via GraphQL e revogar token no Logout")
    void shouldAuthenticateViaHttpOnlyCookieCreateAcidOrderAndRevokeOnLogout() throws Exception {
        // 1. Login emitindo Cookie HttpOnly
        String loginJson = """
                {
                  "email": "alex@ecommerce.com",
                  "password": "Senha@123"
                }
                """;

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.user.email").value("alex@ecommerce.com"))
                .andReturn();

        String setCookieHeader = loginResult.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookieHeader).contains("ACCESS_TOKEN=").contains("HttpOnly");

        Cookie accessTokenCookie = loginResult.getResponse().getCookie("ACCESS_TOKEN");
        assertThat(accessTokenCookie).isNotNull();
        assertThat(accessTokenCookie.isHttpOnly()).isTrue();

        // 2. Mutation GraphQL protegida por Cookie HttpOnly para criar pedido transacional ACID
        String createOrderGraphQl = """
                {
                  "query": "mutation { createOrder(input: { paymentMethod: \\"PIX_INSTANTANEO\\", items: [{ sku: \\"SKU-KEY-MECH-PRO\\", quantity: 2 }] }) { orderCode status totalAmount items { productSku quantity } } }"
                }
                """;

        mockMvc.perform(post("/graphql")
                        .cookie(accessTokenCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createOrderGraphQl))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.createOrder.status").value("PAID_ACID_CONFIRMED"))
                .andExpect(jsonPath("$.data.createOrder.totalAmount").value(1799.8))
                .andExpect(jsonPath("$.data.createOrder.items[0].productSku").value("SKU-KEY-MECH-PRO"));

        // 3. Logout adicionando o JTI do token na Deny List
        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(accessTokenCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false));

        // 4. Tentar reutilizar o mesmo Cookie após o logout deve resultar em sessão anônima (revogado na Deny List!)
        mockMvc.perform(get("/api/v1/auth/me")
                        .cookie(accessTokenCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false));
    }
}

