package ru.eleventh.svmd.services

import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.mindrot.jbcrypt.BCrypt
import ru.eleventh.svmd.exceptions.SvmdException
import ru.eleventh.svmd.model.ApiErrors
import ru.eleventh.svmd.model.db.NewUser
import ru.eleventh.svmd.model.db.User

object UserService {
    private fun hashPassword(password: String): String {
        return BCrypt.hashpw(password, BCrypt.gensalt())
    }

    fun isPasswordMatch(passwordHash: String, password: String): Boolean {
        return BCrypt.checkpw(password, passwordHash)
    }

    fun createUser(email: String, password: String): Long? {
        try {
            return dao.createUser(NewUser(email, hashPassword(password)))
        } catch (e: ExposedSQLException) {
            throw SvmdException(ApiErrors.DUPLICATED_EMAIL)
        }
    }

    fun getUserById(id: Long): User? {
        return dao.getUser(id)
    }

    fun getUserByEmail(email: String): User? {
        return dao.getUserByEmail(email)
    }

    fun updateUser(userId: Long, password: String): Boolean {
        return dao.updateUser(userId, hashPassword(password)) == 1
    }

    fun validatePassword(password: String): Boolean {
        return password.length >= 8
    }
}