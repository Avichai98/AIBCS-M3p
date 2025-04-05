package app.dataservice.boundaries

import java.util.Date

class AlertBoundary(
    var id: String?,
    var type: String?,
    var severity: String?,
    var description: String?,
    var timestamp: Date?,
    var vehicleEntity: VehicleEntity?
) {
    constructor(): this(null, null, null, null, null, null)

    constructor(alertEntity: AlertEntity): this(){
        this.id = alertEntity.id
        this.type = alertEntity.type
        this.severity = alertEntity.severity
        this.description = alertEntity.description
        this.timestamp = alertEntity.timestamp
        this.vehicleEntity = alertEntity.vehicleEntity
    }

    fun toEntity(): AlertEntity{
        val alertEntity = AlertEntity()

        alertEntity.id = id
        alertEntity.type = type
        alertEntity.severity = severity
        alertEntity.description = description
        alertEntity.timestamp = timestamp
        alertEntity.vehicleEntity = vehicleEntity

        return alertEntity
    }

    override fun toString(): String {
        return "AlertEntity(" +
                "id=$id," +
                " type=$type," +
                " severity=$severity," +
                " description=$description," +
                " timestamp=$timestamp," +
                " VehicleEntity=$vehicleEntity)"
    }
}