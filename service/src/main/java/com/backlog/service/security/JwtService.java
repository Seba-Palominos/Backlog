package com.backlog.service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Servicio encargado de generar, firmar y validar los tokens JWT
 * usados para autenticar a los usuarios del backlog.
 */
@Service
public class JwtService {

    private final String secret;
    private final long expiration;

    public JwtService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.expiration}") long expiration) {
        this.secret = secret;
        this.expiration = expiration;
    }

    /**
     * Genera un token JWT para el usuario indicado sin claims adicionales.
     *
     * @param username nombre de usuario
     * @return token JWT firmado
     */
    public String generateToken(String username) {
        return generateToken(new HashMap<>(), username);
    }

    /**
     * Genera un token JWT incluyendo claims adicionales.
     *
     * @param extraClaims claims adicionales a incluir en el token
     * @param username    nombre de usuario
     * @return token JWT firmado
     */
    public String generateToken(Map<String, Object> extraClaims, String username) {
        Instant now = Instant.now();
        return Jwts.builder()
                .claims(extraClaims)
                .subject(username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expiration)))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extrae el nombre de usuario almacenado como sujeto del token.
     *
     * @param token token JWT
     * @return nombre de usuario
     */
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Extrae la fecha de expiración del token.
     *
     * @param token token JWT
     * @return fecha de expiración
     */
    public Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }

    /**
     * Indica si el token es válido (firma correcta y sin expirar).
     *
     * @param token token JWT
     * @return {@code true} si el token es válido
     */
    public boolean isTokenValid(String token) {
        try {
            return !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    /**
     * Indica si el token es válido y corresponde al usuario indicado.
     *
     * @param token    token JWT
     * @param username nombre de usuario esperado
     * @return {@code true} si el token es válido para ese usuario
     */
    public boolean isTokenValid(String token, String username) {
        try {
            String tokenUsername = extractUsername(token);
            return tokenUsername != null
                    && tokenUsername.equals(username)
                    && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    /**
     * Comprueba si el token ya expiró.
     *
     * @param token token JWT
     * @return {@code true} si la fecha de expiración es anterior al instante actual
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Extrae el conjunto completo de claims del token.
     *
     * @param token token JWT
     * @return claims del token
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Construye la clave de firma a partir del secreto configurado.
     *
     * @return clave HMAC para firmar y verificar tokens
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
