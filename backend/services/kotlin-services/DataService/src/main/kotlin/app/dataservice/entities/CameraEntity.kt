package app.dataservice.entities

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "cameras")
class CameraEntity(
    @Id var id: String?,
    var name: String?,
    var emails: List<String>?,
    var location: String?,
    var alertCount: Int?,
    var isActive: Boolean?,
    var status: String?,
    var lastActivity: String?
) {
    constructor() : this(null, null, null, null, null, null, null, null)

    override fun toString(): String {
        return "CameraEntity(" +
                "id=$id," +
                " name=$name," +
                " emails=$emails" +
                " location=$location" +
                " alertCount=$alertCount" +
                " isActive=$isActive" +
                " status=$status" +
                " lastActivity=$lastActivity" +
                ")"
    }
}