package tech.tarakoshka.bridgemich

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName


@Serializable
data class Albums (

  @SerialName("total"  ) var total  : Int?              = null,
  @SerialName("count"  ) var count  : Int?              = null,
  @SerialName("items"  ) var items  : ArrayList<String> = arrayListOf(),
  @SerialName("facets" ) var facets : ArrayList<String> = arrayListOf()

)