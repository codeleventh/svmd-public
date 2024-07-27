package ru.eleventh.svmd.services

import org.jetbrains.exposed.exceptions.ExposedSQLException
import ru.eleventh.svmd.exceptions.SvmdException
import ru.eleventh.svmd.model.ApiErrors
import ru.eleventh.svmd.model.db.NewUser
import ru.eleventh.svmd.model.db.User

object UserService {
    fun createUser(newUser: NewUser): Long? {
        try {
            return dao.createUser(newUser)
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
        return dao.updateUser(userId, password) == 1
    }

    fun validatePassword(password: String): Boolean {
        return password.length >= 8
    }
}