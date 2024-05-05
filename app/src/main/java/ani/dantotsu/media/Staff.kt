package ani.dantotsu.media

import ani.dantotsu.connections.anilist.api.FuzzyDate
import java.io.Serializable

data class Staff(
    val id: Int,
    val name: String?,
    val language: String?,
    val image: String?,
    var description: String? = null,
    var primaryOccupations: List<String>?,
    var gender: String? = null,
    var dateOfBirth: FuzzyDate? = null,
    var dateOfDeath: FuzzyDate? = null,
    var age: Int? = null,
    var startYear: Int? = null,
    var endYear: Int? = null,
    var homeTown: String? = null,
    var bloodType: String? = null,
    var isFav: Boolean
) : Serializable

// languageV2 - apanese, English, Korean, Italian, Spanish, Portuguese, French, German,
// Hebrew, Hungarian, Chinese, Arabic, Filipino, Catalan, Finnish, Turkish, Dutch,
// Swedish, Thai, Tagalog, Malaysian, Indonesian, Vietnamese, Nepali, Hindi, Urdu