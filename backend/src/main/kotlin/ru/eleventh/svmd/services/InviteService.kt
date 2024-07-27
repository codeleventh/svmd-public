package ru.eleventh.svmd.services

import java.util.*

object InviteService {
    fun generateInvites(amount: Int) {
        for (i in 0..amount) {
            dao.createInvite(UUID.randomUUID())
        }
    }

    fun validateInvite(code: UUID): Boolean {
        return dao.validateInvite(code)
    }

    fun useInvite(code: UUID, userId: Long) {
        return dao.useInvite(code, userId)
    }
}