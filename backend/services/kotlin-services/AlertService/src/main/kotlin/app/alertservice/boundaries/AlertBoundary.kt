package app.alertservice.boundaries

import java.time.LocalDateTime

class AlertBoundary(
    var id: String?,
    var cameraId: String?,
    var type: String?,
    var severity: String?,
    var description: String?,
    var timestamp: LocalDateTime?,
    var vehicleBoundary: VehicleBoundary?
) {
    constructor(): this(null, null, null, null, null, null, null)

    override fun toString(): String {
        return "AlertEntity(" +
                "id=$id," +
                " cameraId=$cameraId," +
                " type=$type," +
                " severity=$severity," +
                " description=$description," +
                " timestamp=$timestamp," +
                " VehicleBoundary=$vehicleBoundary)"
    }
}