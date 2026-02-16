package tech.tarakoshka.bridgemich.data.dtos

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import tech.tarakoshka.bridgemich.Albums
import tech.tarakoshka.bridgemich.Assets


@Serializable
data class ImmichSearchResponse (

    @SerialName("albums" ) var albums : Albums? = Albums(),
    @SerialName("assets" ) var assets : Assets? = Assets()

)