package com.csbet.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "CSBET_USERS")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "userSequenceGenerator")
	@SequenceGenerator(
		name = "userSequenceGenerator",
		sequenceName = "SEQ_CSBET_USERS",
		allocationSize = 1
	)
	private Long id;

	@Column(nullable = false, unique = true, length = 50)
	private String username;

	@Column(nullable = false, unique = true, length = 150)
	private String email;

	@Column(nullable = false, length = 255)
	private String passwordHash;

	@Column(nullable = false, precision = 18, scale = 2)
	private BigDecimal balance;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	public User(String username, String email, String passwordHash,
			BigDecimal balance, LocalDateTime createdAt) {
		this.username = username;
		this.email = email;
		this.passwordHash = passwordHash;
		this.balance = balance;
		this.createdAt = createdAt;
	}

	@com.fasterxml.jackson.annotation.JsonIgnore
	public String getPasswordHash() { return passwordHash; }

	public Long getId() { return id; }
	public String getUsername() { return username; }
	public String getEmail() { return email; }
	public BigDecimal getBalance() { return balance; }
	public LocalDateTime getCreatedAt() { return createdAt; }

	public void addCredits(BigDecimal amount) {
		if (amount == null || amount.signum() <= 0 || amount.scale() > 2)
			throw new IllegalArgumentException("Valor de crédito inválido.");
		BigDecimal result = balance.add(amount);
		if (result.compareTo(new BigDecimal("9999999999999999.99")) > 0)
			throw new IllegalArgumentException("Limite de saldo atingido.");
		balance = result;
	}

	protected User() {
	}
}
