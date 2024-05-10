package ani.dantotsu.media

import ani.dantotsu.profile.User
import java.io.Serializable

data class Review (
    val id: Int,
    val userId: Int,
    val mediaId: Int,
    val createdAt: Int,
    val updatedAt: Int,
    val mediaType: String?,
    val summary: String?,
    val body: String?,
    val rating: Int?,
    val score: Int?,
    val user: User?,
    val media: Media?
) : Serializable