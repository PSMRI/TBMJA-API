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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * JWTs revoked at logout. Common-API keeps them in Redis as "denied_&lt;jti&gt;" until they
 * expire; a revoked token is refused here too.
 */
@Component
public class TokenDenylist {
	private static final Logger logger = LoggerFactory.getLogger(TokenDenylist.class);
	private static final String PREFIX = "denied_";

	@Autowired
	private RedisTemplate<String, Object> redisTemplate;

	public boolean isTokenDenylisted(String jti) {
		if (jti == null || jti.trim().isEmpty()) {
			return false;
		}
		try {
			return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX + jti));
		} catch (Exception e) {
			// If Redis is down, do not block every request
			logger.error("Failed to check denylist status for jti {}", jti, e);
			return false;
		}
	}
}
