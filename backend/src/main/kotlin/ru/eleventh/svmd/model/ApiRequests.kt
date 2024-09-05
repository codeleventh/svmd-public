package ru.eleventh.svmd.model

import java.util.*

data class RegistrationRequest(val email: String, val password: String, val invite: UUID)
data class ChangeProfileRequest(val password: String)
data class GenerateInvitesRequest(val amount: Int)