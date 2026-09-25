package com.partyplanner.services

import com.partyplanner.TestDatabaseSetup
import com.partyplanner.dto.LoginRequest
import com.partyplanner.dto.RegisterRequest
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class AuthServiceTest {

    private val service = AuthService(
        jwtSecret = "test-secret-key-long-enough-for-hmac256",
        jwtIssuer = "http://localhost",
        jwtAudience = "partyplanner",
    )

    @BeforeTest
    fun setUp() {
        TestDatabaseSetup.init()
    }

    @AfterTest
    fun tearDown() {
        TestDatabaseSetup.clearAll()
    }

    @Test
    fun `register creates user and returns token and displayName`() = runTest {
        val response = service.register(
            RegisterRequest(email = "alice@example.com", password = "password123", displayName = "Alice")
        )
        assertTrue(response.token.isNotEmpty())
        assertEquals("Alice", response.displayName)
        assertTrue(response.userId > 0)
    }

    @Test
    fun `register with duplicate email throws`() = runTest {
        service.register(RegisterRequest("alice@example.com", "password123", "Alice"))
        assertFailsWith<IllegalArgumentException> {
            service.register(RegisterRequest("alice@example.com", "other", "Alice2"))
        }
    }

    @Test
    fun `login with valid credentials returns token`() = runTest {
        service.register(RegisterRequest("alice@example.com", "password123", "Alice"))
        val response = service.login(LoginRequest("alice@example.com", "password123"))
        assertTrue(response.token.isNotEmpty())
        assertEquals("Alice", response.displayName)
    }

    @Test
    fun `login with wrong password throws`() = runTest {
        service.register(RegisterRequest("alice@example.com", "password123", "Alice"))
        assertFailsWith<IllegalStateException> {
            service.login(LoginRequest("alice@example.com", "wrongpassword"))
        }
    }

    @Test
    fun `login with unknown email throws`() = runTest {
        assertFailsWith<IllegalStateException> {
            service.login(LoginRequest("nobody@example.com", "password"))
        }
    }

    @Test
    fun `verifyToken returns userId for valid token`() = runTest {
        val registered = service.register(
            RegisterRequest("alice@example.com", "password123", "Alice")
        )
        val userId = service.verifyToken(registered.token)
        assertNotNull(userId)
        assertEquals(registered.userId, userId)
    }

    @Test
    fun `verifyToken returns null for invalid token`() {
        assertNull(service.verifyToken("not.a.valid.token"))
    }

    @Test
    fun `verifyToken returns null for empty string`() {
        assertNull(service.verifyToken(""))
    }
}
