/*
 * AMRIT – Accessible Medical Records via Integrated Technology
 * Integrated EHR (Electronic Health Records) Solution
 *
 * Copyright (C) "Piramal Swasthya Management and Research Institute"
 *
 * This file is part of AMRIT.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see https://www.gnu.org/licenses/.
 */
package com.iemr.tbmja.utils;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Checks JWTs issued by Common-API at login, signed with {@code jwt.secret} (shared by all AMRIT
 * APIs). The token carries the user's id in the "userId" claim and the user name as subject.
 */
@Component
public class JwtUtil {

	@Value("${jwt.secret}")
	private String secretKey;


	@Autowired
	private TokenDenylist tokenDenylist;

	private SecretKey getSigningKey() {
		if (secretKey == null || secretKey.isEmpty()) {
			throw new IllegalStateException("JWT secret key is not set (jwt.secret)");
		}
		return Keys.hmacShaKeyFor(secretKey.getBytes());
	}

	/** The token's claims, or null if it is invalid, expired or revoked. */
	public Claims validateToken(String token) {
		try {
			Claims claims = Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
			if (claims.getId() != null && tokenDenylist.isTokenDenylisted(claims.getId())) {
				return null;
			}
			return claims;
		} catch (Exception e) {
			return null;
		}
	}
}
