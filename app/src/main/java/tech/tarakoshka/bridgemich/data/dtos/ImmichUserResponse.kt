package tech.tarakoshka.bridgemich.data.dtos

import kotlinx.serialization.Serializable

@Serializable
data class ImmichUserResponse(
    val email: String
)
