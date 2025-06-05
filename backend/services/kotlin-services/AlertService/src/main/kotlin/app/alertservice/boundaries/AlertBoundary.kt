package app.alertservice.boundaries

import java.util.Date

class AlertBoundary(
    var id: String?,
    var type: String?,
    var severity: String?,
    var description: String?,
    var timestamp: Date?,
    var vehicleBoundary: VehicleBoundary?
) {
    constructor(): this(null, null, null, null, null, null)

    override fun toString(): String {
        return "AlertEntity(" +
                "id=$id," +
                " type=$type," +
                " severity=$severity," +
                " description=$description," +
                " timestamp=$timestamp," +
                " VehicleBoundary=$vehicleBoundary)"
    }
}