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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.iemr.tbmja.utils.exception.IEMRException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;

class JwtUserIdValidationFilterTest {

	private JwtAuthenticationUtil jwtAuthenticationUtil;
	private JwtUserIdValidationFilter filter;
	private MockHttpServletResponse response;

	@BeforeEach
	void setUp() {
		jwtAuthenticationUtil = mock(JwtAuthenticationUtil.class);
		filter = new JwtUserIdValidationFilter(jwtAuthenticationUtil, new CookieUtil(), "http://localhost:*");
		response = new MockHttpServletResponse();
	}

	private static MockHttpServletRequest request(String method, String path) {
		MockHttpServletRequest request = new MockHttpServletRequest(method, path);
		request.setRequestURI(path);
		return request;
	}

	@Test
	void publicPathsNeedNoToken() throws Exception {
		for (String path : new String[] { "/health", "/version", "/swagger-ui/index.html", "/v3/api-docs" }) {
			FilterChain chain = mock(FilterChain.class);
			MockHttpServletRequest request = request("GET", path);
			filter.doFilter(request, response, chain);
			verify(chain).doFilter(request, response);
		}
	}

	@Test
	void missingTokenIsRejected() throws Exception {
		MockFilterChain chain = new MockFilterChain();
		filter.doFilter(request("GET", "/worklist"), response, chain);
		assertEquals(401, response.getStatus());
		assertEquals(null, chain.getRequest());
	}

	@Test
	void invalidTokenIsRejected() throws Exception {
		when(jwtAuthenticationUtil.validateUserIdAndJwtToken("bad")).thenThrow(new IEMRException("Invalid JWT token."));
		MockHttpServletRequest request = request("GET", "/worklist");
		request.addHeader("Jwttoken", "bad");
		FilterChain chain = mock(FilterChain.class);

		filter.doFilter(request, response, chain);

		assertEquals(401, response.getStatus());
		verify(chain, never()).doFilter(request, response);
	}

	@Test
	void validCommonApiTokenFromCookieIsAccepted() throws Exception {
		when(jwtAuthenticationUtil.validateUserIdAndJwtToken("good")).thenReturn(true);
		MockHttpServletRequest request = request("GET", "/worklist");
		request.setCookies(new Cookie("Jwttoken", "good"));
		FilterChain chain = mock(FilterChain.class);

		filter.doFilter(request, response, chain);

		verify(chain).doFilter(request, response);
	}

	@Test
	void preflightFromAllowedOriginIsAnswered() throws Exception {
		MockHttpServletRequest request = request("OPTIONS", "/worklist");
		request.addHeader("Origin", "http://localhost:4200");
		FilterChain chain = mock(FilterChain.class);

		filter.doFilter(request, response, chain);

		assertEquals(200, response.getStatus());
		assertEquals("http://localhost:4200", response.getHeader("Access-Control-Allow-Origin"));
		verify(chain, never()).doFilter(request, response);
	}
}
