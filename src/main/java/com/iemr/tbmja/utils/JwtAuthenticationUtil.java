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

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.iemr.tbmja.data.iemr.User;
import com.iemr.tbmja.repo.iemr.UserRepo;
import com.iemr.tbmja.utils.exception.IEMRException;

import io.jsonwebtoken.Claims;

/**
 * Checks that a JWT is valid and that its user still exists and is not deleted.
 * The user is looked up in the Redis cache ("user_&lt;userId&gt;") first, then in m_user.
 */
@Component
public class JwtAuthenticationUtil {
	private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationUtil.class);
	private static final String USER_KEY_PREFIX = "user_";

	@Autowired
	private JwtUtil jwtUtil;

	@Autowired
	private RedisTemplate<String, Object> redisTemplate;

	@Autowired
	private UserRepo userRepo;

	public boolean validateUserIdAndJwtToken(String jwtToken) throws IEMRException {
		Claims claims = jwtUtil.validateToken(jwtToken);
		if (claims == null) {
			throw new IEMRException("Invalid JWT token.");
		}
		String userId = claims.get("userId", String.class);
		if (userId == null) {
			throw new IEMRException("JWT token has no userId.");
		}
		User user = getUserFromCache(userId);
		if (user == null) {
			user = fetchUserFromDB(userId);
		}
		if (user == null) {
			throw new IEMRException("Invalid User ID or user is deactivated, please contact the admin.");
		}
		return true;
	}

	private User getUserFromCache(String userId) {
		try {
			return (User) redisTemplate.opsForValue().get(USER_KEY_PREFIX + userId);
		} catch (Exception e) {
			logger.warn("Could not read user {} from Redis: {}", userId, e.getMessage());
			return null;
		}
	}

	private User fetchUserFromDB(String userId) {
		User user = userRepo.findActiveUser(Integer.parseInt(userId));
		if (user == null) {
			logger.warn("User not found for userId {}", userId);
			return null;
		}
		try {
			User cached = new User();
			cached.setUserID(user.getUserID());
			cached.setUserName(user.getUserName());
			redisTemplate.opsForValue().set(USER_KEY_PREFIX + userId, cached, 30, TimeUnit.MINUTES);
		} catch (Exception e) {
			logger.warn("Could not cache user {} in Redis: {}", userId, e.getMessage());
		}
		return user;
	}
}
