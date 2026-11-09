package com.ecommerce.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * ADR-005: Filtro de autenticação Stateless que extrai o JWT prioritariamente
 * de Cookies HttpOnly (protegendo contra XSS no navegador) e valida criptograficamente
 * a assinatura sem precisar consultar o PostgreSQL a cada requisição.
 */
@Component
public class HttpOnlyCookieJwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final TokenDenyListService tokenDenyListService;

    public HttpOnlyCookieJwtFilter(JwtService jwtService, TokenDenyListService tokenDenyListService) {
        this.jwtService = jwtService;
        this.tokenDenyListService = tokenDenyListService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        TokenExtraction extraction = extractToken(request);

        if (extraction != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                Claims claims = jwtService.parseAndValidateClaims(extraction.token());
                String jti = claims.getId();
                if (!tokenDenyListService.isRevoked(jti)) {
                    AuthenticatedUserPrincipal principal = jwtService.toPrincipal(claims, extraction.source());
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            List.of(new SimpleGrantedAuthority(principal.role()))
                    );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception ignored) {
                // Token expirado ou assinatura inválida: segue como anônimo sem bloquear rotas públicas
            }
        }

        filterChain.doFilter(request, response);
    }

    public TokenExtraction extractToken(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (jwtService.getCookieName().equals(cookie.getName())
                        && cookie.getValue() != null
                        && !cookie.getValue().isBlank()) {
                    return new TokenExtraction(cookie.getValue(), "OAUTH2_HTTP_ONLY_COOKIE");
                }
            }
        }
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String bearer = authHeader.substring(7).trim();
            if (!bearer.isEmpty()) {
                return new TokenExtraction(bearer, "OAUTH2_BEARER_HEADER");
            }
        }
        return null;
    }

    public record TokenExtraction(String token, String source) {}
}

