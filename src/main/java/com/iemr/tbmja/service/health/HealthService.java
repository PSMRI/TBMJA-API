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
package com.iemr.tbmja.service.health;

import java.sql.Connection;
import java.sql.Statement;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;

/**
 * Checks the services this API depends on: db_iemr and Redis.
 * Overall status is UP when all are up, else DOWN.
 */
@Service
public class HealthService {
	private static final Logger logger = LoggerFactory.getLogger(HealthService.class);
	private static final String STATUS = "status";
	private static final String UP = "UP";
	private static final String DOWN = "DOWN";
	private static final int DB_TIMEOUT_SECONDS = 3;

	private final DataSource dataSource;
	private final RedisConnectionFactory redisConnectionFactory;

	public HealthService(DataSource dataSource, RedisConnectionFactory redisConnectionFactory) {
		this.dataSource = dataSource;
		this.redisConnectionFactory = redisConnectionFactory;
	}

	public Map<String, Object> checkHealth() {
		Map<String, Object> components = new LinkedHashMap<>();
		components.put("mysql", checkDatabase(dataSource, "db_iemr"));
		components.put("redis", checkRedis());

		boolean allUp = components.values().stream().allMatch(c -> UP.equals(((Map<?, ?>) c).get(STATUS)));

		Map<String, Object> response = new LinkedHashMap<>();
		response.put(STATUS, allUp ? UP : DOWN);
		response.put("timestamp", Instant.now().toString());
		response.put("components", components);
		return response;
	}

	private Map<String, Object> checkDatabase(DataSource dataSource, String name) {
		long start = System.currentTimeMillis();
		try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
			statement.setQueryTimeout(DB_TIMEOUT_SECONDS);
			statement.execute("SELECT 1");
			return result(UP, start, null);
		} catch (Exception e) {
			logger.warn("Health check failed for {}: {}", name, e.getMessage());
			return result(DOWN, start, "Database not reachable");
		}
	}

	private Map<String, Object> checkRedis() {
		long start = System.currentTimeMillis();
		try (RedisConnection connection = redisConnectionFactory.getConnection()) {
			connection.ping();
			return result(UP, start, null);
		} catch (Exception e) {
			logger.warn("Health check failed for Redis: {}", e.getMessage());
			return result(DOWN, start, "Redis not reachable");
		}
	}

	private Map<String, Object> result(String status, long start, String error) {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put(STATUS, status);
		result.put("responseTimeMs", System.currentTimeMillis() - start);
		if (error != null) {
			result.put("error", error);
		}
		return result;
	}
}
