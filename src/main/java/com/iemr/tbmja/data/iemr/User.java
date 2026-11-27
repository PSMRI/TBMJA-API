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
package com.iemr.tbmja.data.iemr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * Login account (db_iemr.m_user), read-only here. Only the columns needed to check a JWT's user.
 * Also the value type of the Redis user cache, which other AMRIT APIs write too, so unknown
 * JSON fields are ignored.
 */
@Data
@Entity
@Table(name = "m_user")
@JsonIgnoreProperties(ignoreUnknown = true)
public class User {

	@Id
	@Column(name = "UserID", insertable = false, updatable = false)
	private Integer userID;

	@Column(name = "UserName", insertable = false, updatable = false)
	private String userName;

	@Column(name = "StatusID", insertable = false, updatable = false)
	private Integer statusID;

	@Column(name = "Deleted", insertable = false, updatable = false)
	private Boolean deleted;
}
