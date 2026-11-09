package com.ecommerce.security;

import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ADR-005 (Consequência): Como o JWT é stateless, invalidar um token antes da expiração
 * (ex: logout forçado ou revogação de sessão) exige uma Deny List em memória/cache.
 */
@Service
public class TokenDenyListService {

    private final Map<String, Instant> revokedTokens = new ConcurrentHashMap<>();

    public void revokeToken(String jti, Instant expiresAt) {
        if (jti != null && !jti.isBlank()) {
            revokedTokens.put(jti, expiresAt != null ? expiresAt : Instant.now().plusSeconds(86400));
            cleanupExpired();
        }
    }

    public boolean isRevoked(String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }
        Instant expiry = revokedTokens.get(jti);
        if (expiry == null) {
            return false;
        }
        if (expiry.isBefore(Instant.now())) {
            revokedTokens.remove(jti);
            return false;
        }
        return true;
    }

    public int getActiveRevokedCount() {
        cleanupExpired();
        return revokedTokens.size();
    }

    private void cleanupExpired() {
        Instant now = Instant.now();
        revokedTokens.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
    }
}

