package com.empresa.actas.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Service
public class JwtService {

    /**
     * SEC-011: limite duro de duracion del JWT en codigo (8 h). El yaml queda
     * intacto (fuera de alcance del sprint): si la config declara mas, se
     * recorta; si declara menos, se respeta. El operador puede bajar el tope
     * con JWT_EXPIRATION (equivale a una sesion mas corta).
     */
    private static final long MAX_EXPIRACION_MS = 8L * 60 * 60 * 1000;

    private final SecretKey signingKey;
    private final long expiration;
    private final JwtRevocadoRepository revocadoRepository;

    public JwtService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.expiration}") long expiration,
            JwtRevocadoRepository revocadoRepository) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expiration = Math.min(expiration, MAX_EXPIRACION_MS);
        this.revocadoRepository = revocadoRepository;
    }

    public String generarToken(UserDetails userDetails) {
        return generarToken(new HashMap<>(), userDetails);
    }

    public String generarToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        // SEC-011: jti unico por token. Es la clave de la denylist que hace
        // efectivo el logout: sin jti no habria como revocar un JWT concreto.
        return Jwts.builder()
                .claims(extraClaims)
                .id(UUID.randomUUID().toString())
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(signingKey)
                .compact();
    }

    public String extraerUsername(String token) {
        return extraerClaim(token, Claims::getSubject);
    }

    public String extraerJti(String token) {
        return extraerClaim(token, Claims::getId);
    }

    /** Expiracion del JWT (para guardar en la denylist y poder podarla). */
    public Date extraerExpiracion(String token) {
        return extraerClaim(token, Claims::getExpiration);
    }

    public boolean validarToken(String token, UserDetails userDetails) {
        final String username = extraerUsername(token);
        if (!username.equals(userDetails.getUsername()) || estaExpirado(token)) {
            return false;
        }
        // SEC-011: token revocado (logout efectivo / sesion terminada) deja de
        // validarse aunque no haya expirado. Este hook lo ejecuta el
        // JwtAuthenticationFilter via validarToken, sin tocar el filtro.
        final String jti = extraerJti(token);
        return jti == null || !revocadoRepository.existsByJti(jti);
    }

    private <T> T extraerClaim(String token, Function<Claims, T> resolver) {
        final Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return resolver.apply(claims);
    }

    private boolean estaExpirado(String token) {
        return extraerClaim(token, Claims::getExpiration).before(new Date());
    }
}