package ani.dantotsu.media.cereal

import ani.dantotsu.media.cereal.Media
import java.io.Serializable

data class Studio(
    val id: String,
    val name: String,
    var yearMedia: MutableMap<String, ArrayList<Media>>? = null
) : Serializable
