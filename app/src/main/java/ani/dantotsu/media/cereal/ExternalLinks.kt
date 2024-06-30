package ani.dantotsu.media.cereal

import java.io.Serializable

data class ExternalLinks(
    var youtube: String? = null,
    var crunchyroll: String? = null,
    var hulu: String? = null,
    var vrv: String? = null,
    var disney: String? = null,
    var netflix: String? = null,
    var max: String? = null
) : Serializable