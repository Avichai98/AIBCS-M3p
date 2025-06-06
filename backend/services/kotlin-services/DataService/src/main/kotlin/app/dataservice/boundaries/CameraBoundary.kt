package app.dataservice.boundaries

import app.dataservice.entities.CameraEntity

class CameraBoundary(
    var id: String?,
    var name: String?,
    var emails: List<String>?,
    var location: String?,
    var alertCount: Int?,
    var isActive: Boolean?,
    var status: String?,
    var lastActivity: String?
) {
    constructor() : this(null, null, null, null, null, null, null, null)

    constructor(cameraEntity: CameraEntity) : this() {
        this.id = cameraEntity.id
        this.name = cameraEntity.name
        this.emails = cameraEntity.emails
        this.location = cameraEntity.location
        this.alertCount = cameraEntity.alertCount
        this.isActive = cameraEntity.isActive
        this.status = cameraEntity.status
        this.lastActivity = cameraEntity.lastActivity
    }

    fun toEntity(): CameraEntity {
        val cameraEntity = CameraEntity()

        cameraEntity.id = id
        cameraEntity.name = name
        cameraEntity.emails = emails
        cameraEntity.location = location
        cameraEntity.alertCount = alertCount
        cameraEntity.isActive = isActive
        cameraEntity.status = status
        cameraEntity.lastActivity = lastActivity

        return cameraEntity
    }

    override fun toString(): String {
        return "CameraBoundary(" +
                "id=$id," +
                " name=$name," +
                " emails=$emails," +
                " location=$location," +
                " alertCount=$alertCount," +
                " isActive=$isActive," +
                " status=$status," +
                " lastActivity=$lastActivity" +
                ")"
    }
}
