package com.partyplanner.services

import com.partyplanner.TestDatabaseSetup
import com.partyplanner.createResetToken
import com.partyplanner.createTestUser
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours

class PasswordResetServiceTest {

    private val service = PasswordResetService(
        resendApiKey = "",
        fromEmail = "noreply@test.com",
        appBaseUrl = "http://localhost",
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
    fun `resetPassword updates password hash for valid token`() = runTest {
        val userId = createTestUser(password = "oldPassword")
        val token = createResetToken(userId)

        service.resetPassword(token, "newPassword123")

        val hash = transaction {
            com.partyplanner.db.tables.UserEntity.findById(userId)!!.passwordHash
        }
        assertTrue(BCrypt.checkpw("newPassword123", hash))
    }

    @Test
    fun `resetPassword marks token as used`() = runTest {
        val userId = createTestUser()
        val token = createResetToken(userId)

        service.resetPassword(token, "newPassword123")

        assertFailsWith<IllegalArgumentException> {
            service.resetPassword(token, "anotherPassword")
        }
    }

    @Test
    fun `resetPassword rejects already-used token`() = runTest {
        val userId = createTestUser()
        val token = createResetToken(userId)

        service.resetPassword(token, "firstPass123")

        assertFailsWith<IllegalArgumentException> {
            service.resetPassword(token, "secondPass123")
        }
    }

    @Test
    fun `resetPassword rejects expired token`() = runTest {
        val userId = createTestUser()
        val token = createResetToken(userId, expiresIn = (-1).hours)

        assertFailsWith<IllegalArgumentException> {
            service.resetPassword(token, "newPassword123")
        }
    }

    @Test
    fun `resetPassword rejects invalid token`() = runTest {
        assertFailsWith<IllegalStateException> {
            service.resetPassword("nonexistent-token", "newPassword123")
        }
    }

    @Test
    fun `resetPassword rejects password shorter than 8 chars`() = runTest {
        val userId = createTestUser()
        val token = createResetToken(userId)

        assertFailsWith<IllegalArgumentException> {
            service.resetPassword(token, "short")
        }
    }

    @Test
    fun `requestReset silently ignores unknown email`() = runTest {
        service.requestReset("nobody@test.com")
        // No exception — silent for security
    }
}
