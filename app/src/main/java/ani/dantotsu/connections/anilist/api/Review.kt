package ani.dantotsu.connections.anilist.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Review (
    @SerialName("id") var id: Int,
    @SerialName("userId") var userId: Int,
    @SerialName("mediaId") var mediaId: Int,
    @SerialName("createdAt") var createdAt: Int,
    @SerialName("updatedAt") var updatedAt: Int,
    @SerialName("mediaType") var mediaType: String?,
    @SerialName("summary") var summary: String?,
    @SerialName("body") var body: String?,
    @SerialName("rating") var rating: Int?,
    @SerialName("score") var score: Int?
)