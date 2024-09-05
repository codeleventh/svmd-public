package ru.eleventh.svmd.services

import java.util.*

object InviteService {
    fun generateInvites(amount: Int): List<UUID> {
        val invites = (1..amount).map { UUID.randomUUID() }
        invites.forEach { dao.createInvite(it) }
        return invites
    }

    fun validateInvite(code: UUID): Boolean {
        return dao.validateInvite(code)
    }

    fun useInvite(code: UUID, userId: Long) {
        return dao.useInvite(code, userId)
    }
}