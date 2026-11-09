package com.ecommerce;

import com.ecommerce.domain.UserEntity;
import com.ecommerce.security.AuthenticatedUserPrincipal;
import com.ecommerce.security.JwtService;
import com.ecommerce.security.TokenDenyListService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAndDenyListUnitTest {

    private static final String TEST_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    @Test
    @DisplayName("Deve emitir e validar criptograficamente o JWT Stateless sem consultar banco de dados")
    void shouldGenerateAndValidateStatelessJwt() {
        JwtService jwtService = new JwtService(TEST_SECRET, 3600000L, "ACCESS_TOKEN", false);
        UserEntity user = new UserEntity("Alexsandro Clemente", "alex@ecommerce.com", "hash", "ROLE_CUSTOMER", "PRIME_BLACK");
        user.setId(42L);

        String token = jwtService.generateToken(user);
        assertThat(token).isNotBlank();

        Claims claims = jwtService.parseAndValidateClaims(token);
        AuthenticatedUserPrincipal principal = jwtService.toPrincipal(claims, "OAUTH2_HTTP_ONLY_COOKIE");

        assertThat(principal.userId()).isEqualTo(42L);
        assertThat(principal.email()).isEqualTo("alex@ecommerce.com");
        assertThat(principal.role()).isEqualTo("ROLE_CUSTOMER");
        assertThat(principal.planTier()).isEqualTo("PRIME_BLACK");
        assertThat(principal.tokenJti()).isNotBlank();
    }

    @Test
    @DisplayName("Deve revogar token na Deny List imediatamente durante o logout forçado")
    void shouldRevokeTokenInDenyList() {
        TokenDenyListService denyList = new TokenDenyListService();
        String jti = "jti-12345-revoked";

        assertThat(denyList.isRevoked(jti)).isFalse();
        denyList.revokeToken(jti, Instant.now().plusSeconds(600));

        assertThat(denyList.isRevoked(jti)).isTrue();
        assertThat(denyList.getActiveRevokedCount()).isEqualTo(1);
    }
}

