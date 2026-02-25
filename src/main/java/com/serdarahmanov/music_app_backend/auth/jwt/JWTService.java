package com.serdarahmanov.music_app_backend.auth.jwt;

import com.serdarahmanov.music_app_backend.utility.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Locator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;

import java.util.Date;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class JWTService {

    private final JwtProperties jwtProperties;
    private final JwtKeyProvider jwtKeyProvider;

    // -------------------------------
    // GENERATE TOKEN
    // -------------------------------
    public String generateToken(UserDetails userDetails) {
        return generateToken(Map.of(), userDetails); // empty extra claims
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .header().keyId(jwtKeyProvider.getCurrentKid()).and()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuer(jwtProperties.getIssuer())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.getExpiration()))
                .signWith(jwtKeyProvider.getPrivateKey(), Jwts.SIG.RS256)
                .compact();
    }

    // -------------------------------
    // EXTRACT USERNAME
    // -------------------------------
    public Optional<String> extractUsername(String token) {
        return extractAllClaims(token).map(Claims::getSubject);
    }

    // -------------------------------
    // VALIDATE TOKEN
    // -------------------------------
    public boolean isTokenValid(String token, UserDetails userDetails) {

        return extractAllClaims(token).map(
                claims ->
                    userDetails.getUsername().equals(claims.getSubject())&&
                            jwtProperties.getIssuer().equals(claims.getIssuer()) &&
                            claims.getExpiration()!=null &&
                            claims.getExpiration().after(new Date())

        ).orElse(false);
    }

    // -------------------------------
    // EXTRACT CLAIMS
    // -------------------------------
    public Optional<Claims> extractAllClaims(String token) {
       try{ Claims claims = Jwts.parser()
                .keyLocator(new Locator<Key>() {
                    @Override
                    public Key locate(Header header) {
                        Object kidValue = header.get("kid");
                        String kid = kidValue == null ? null : kidValue.toString();
                        return jwtKeyProvider.getVerificationKeyForKid(kid);
                    }
                })
                .build()
                .parseSignedClaims(token)
                .getPayload();
           return Optional.of(claims);
       } catch (JwtException | IllegalArgumentException ex){
           return Optional.empty();
       }


    }
}
