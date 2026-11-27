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

import java.io.IOException;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Lets a request through only with a valid JWT, taken from the "Jwttoken" cookie or the
 * "Jwttoken" header. Users log in through Common-API, which issues the token. Public paths
 * (health, version, Swagger, /public/**) are skipped.
 * Also answers CORS preflight (OPTIONS) requests.
 */
@Component
@Order(2)
public class JwtUserIdValidationFilter implements Filter {
	private static final Logger logger = LoggerFactory.getLogger(JwtUserIdValidationFilter.class);

	private final JwtAuthenticationUtil jwtAuthenticationUtil;
	private final CookieUtil cookieUtil;
	private final String allowedOrigins;

	public JwtUserIdValidationFilter(JwtAuthenticationUtil jwtAuthenticationUtil, CookieUtil cookieUtil,
			@Value("${cors.allowed-origins}") String allowedOrigins) {
		this.jwtAuthenticationUtil = jwtAuthenticationUtil;
		this.cookieUtil = cookieUtil;
		this.allowedOrigins = allowedOrigins;
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
			throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) servletRequest;
		HttpServletResponse response = (HttpServletResponse) servletResponse;

		String origin = request.getHeader("Origin");
		if (origin != null && isOriginAllowed(origin)) {
			response.setHeader("Access-Control-Allow-Origin", origin);
			response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
			response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, Accept, Jwttoken");
			response.setHeader("Access-Control-Allow-Credentials", "true");
		}

		if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
			response.setStatus(HttpServletResponse.SC_OK);
			return;
		}

		if (isPublicPath(request.getRequestURI(), request.getContextPath())) {
			filterChain.doFilter(servletRequest, servletResponse);
			return;
		}

		String jwtToken = cookieUtil.getCookieValue(request, CookieUtil.JWT_COOKIE)
				.orElse(request.getHeader(CookieUtil.JWT_COOKIE));
		if (jwtToken == null || jwtToken.isEmpty()) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized: missing token");
			return;
		}
		try {
			jwtAuthenticationUtil.validateUserIdAndJwtToken(jwtToken);
		} catch (Exception e) {
			logger.warn("Rejected request to {}: {}", request.getRequestURI(), e.getMessage());
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized: invalid token");
			return;
		}
		filterChain.doFilter(servletRequest, servletResponse);
	}

	private boolean isPublicPath(String path, String contextPath) {
		return path.equals(contextPath + "/health") || path.equals(contextPath + "/version")
				|| path.startsWith(contextPath + "/swagger-ui") || path.startsWith(contextPath + "/v3/api-docs")
				|| path.startsWith(contextPath + "/public");
	}

	private boolean isOriginAllowed(String origin) {
		if (allowedOrigins == null || allowedOrigins.trim().isEmpty()) {
			return false;
		}
		return Arrays.stream(allowedOrigins.split(",")).map(String::trim).anyMatch(pattern -> {
			String regex = pattern.replace(".", "\\.").replace("*", ".*").replace("http://localhost:.*",
					"http://localhost:\\d+");
			return origin.matches(regex);
		});
	}
}
