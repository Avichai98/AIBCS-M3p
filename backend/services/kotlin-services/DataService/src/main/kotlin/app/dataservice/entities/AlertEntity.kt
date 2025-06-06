package app.dataservice.entities

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.util.*

@Document (collection = "Alerts")
class AlertEntity(
    @Id var id: String?,
    var cameraId: String?,
    var type: String?,
    var severity: String?,
    var description: String?,
    var timestamp: Date?,
    var vehicleEntity: VehicleEntity?
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
                " VehicleEntity=$vehicleEntity)"
    }
}