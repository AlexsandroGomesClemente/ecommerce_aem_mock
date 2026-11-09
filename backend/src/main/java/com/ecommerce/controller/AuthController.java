package com.ecommerce.controller;

import com.ecommerce.domain.UserEntity;
import com.ecommerce.dto.ApiDtos.AuthStatusResponse;
import com.ecommerce.dto.ApiDtos.LoginRequest;
import com.ecommerce.dto.ApiDtos.UserProfileView;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.security.AuthenticatedUserPrincipal;
import com.ecommerce.security.HttpOnlyCookieJwtFilter;
import com.ecommerce.security.JwtService;
import com.ecommerce.security.TokenDenyListService;
import com.ecommerce.telemetry.TelemetryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Optional;

/**
 * Autenticação Stateless OAuth 2.0 + JWT entregue exclusivamente via Cookies HttpOnly e Secure.
 * O token não é exposto ao localStorage do navegador, blindando a aplicação contra roubo de sessão via XSS.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenDenyListService tokenDenyListService;
    private final HttpOnlyCookieJwtFilter cookieJwtFilter;
    private final TelemetryService telemetryService;

    public AuthController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            TokenDenyListService tokenDenyListService,
            HttpOnlyCookieJwtFilter cookieJwtFilter,
            TelemetryService telemetryService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenDenyListService = tokenDenyListService;
        this.cookieJwtFilter = cookieJwtFilter;
        this.telemetryService = telemetryService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthStatusResponse> login(@Valid @RequestBody LoginRequest request) {
        Optional<UserEntity> userOpt = userRepository.findByEmailIgnoreCase(request.email().trim());

        if (userOpt.isEmpty() || !passwordEncoder.matches(request.password(), userOpt.get().getPasswordHash())) {
            telemetryService.appendLog("WARN", "AuthController[OAuth2]", "Falha de login para " + request.email(), 4);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthStatusResponse(
                            false,
                            "Credenciais inválidas. Verifique seu e-mail e senha.",
                            "HttpOnly; Secure; SameSite=Lax",
                            null
                    ));
        }

        UserEntity user = userOpt.get();
        String jwt = jwtService.generateToken(user);

        ResponseCookie cookie = ResponseCookie.from(jwtService.getCookieName(), jwt)
                .httpOnly(true)
                .secure(jwtService.isCookieSecure())
                .path("/")
                .maxAge(jwtService.getExpirationSeconds())
                .sameSite("Lax")
                .build();

        UserProfileView profile = new UserProfileView(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getPlanTier(),
                "OAUTH2_HTTP_ONLY_COOKIE"
        );

        telemetryService.appendLog("SECURITY", "AuthController[JWT]", "Sessão Stateless emitida via Cookie HttpOnly para " + user.getEmail(), 5);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new AuthStatusResponse(
                        true,
                        "Autenticado via OAuth 2.0 + JWT em Cookie HttpOnly",
                        "HttpOnly; SameSite=Lax; Max-Age=" + jwtService.getExpirationSeconds(),
                        profile
                ));
    }

    @PostMapping("/logout")
    public ResponseEntity<AuthStatusResponse> logout(HttpServletRequest request) {
        HttpOnlyCookieJwtFilter.TokenExtraction extraction = cookieJwtFilter.extractToken(request);
        if (extraction != null) {
            try {
                String jti = jwtService.extractJti(extraction.token());
                Instant exp = jwtService.extractExpiration(extraction.token());
                tokenDenyListService.revokeToken(jti, exp);
                telemetryService.appendLog("SECURITY", "TokenDenyList", "Token JTI=" + jti + " adicionado à Deny List até expiração", 2);
            } catch (Exception ignored) {
            }
        }

        ResponseCookie clearCookie = ResponseCookie.from(jwtService.getCookieName(), "")
                .httpOnly(true)
                .secure(jwtService.isCookieSecure())
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();

        SecurityContextHolder.clearContext();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                .body(new AuthStatusResponse(
                        false,
                        "Logout concluído: Cookie HttpOnly removido e JWT incluído na Deny List.",
                        "HttpOnly; Max-Age=0 (Revoked)",
                        null
                ));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthStatusResponse> currentSession() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUserPrincipal principal) {
            UserProfileView profile = new UserProfileView(
                    principal.userId(),
                    principal.name(),
                    principal.email(),
                    principal.role(),
                    principal.planTier(),
                    principal.authSource()
            );
            return ResponseEntity.ok(new AuthStatusResponse(
                    true,
                    "Sessão Stateless validada criptograficamente sem consulta ao PostgreSQL.",
                    "HttpOnly; SameSite=Lax",
                    profile
            ));
        }

        return ResponseEntity.ok(new AuthStatusResponse(
                false,
                "Usuário não autenticado.",
                "HttpOnly; SameSite=Lax",
                null
        ));
    }
}

