package ani.dantotsu.media.reviews

import ani.dantotsu.media.Media
import ani.dantotsu.profile.User
import java.io.Serializable

data class Review (
    val id: Int,
    val userId: Int,
    val mediaId: Int,
    val mediaType: String?,
    val summary: String?,
    val body: String?,
    var rating: Int?,
    var ratingAmount: Int?,
    var userRating: String?,
    val score: Int?,
    val createdAt: Int,
    val updatedAt: Int,
    val user: User?,
    val media: Media?
) : Serializable