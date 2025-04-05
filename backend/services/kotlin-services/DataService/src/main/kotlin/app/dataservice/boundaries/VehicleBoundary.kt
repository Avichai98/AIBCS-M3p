package app.dataservice.boundaries

import java.util.Date

class VehicleBoundary(
    var id: String?,
    var type: String?,
    var model: String?,
    var manufacturer: String?,
    var color: String?,
    var imageUrl: String?,
    var description: String?,
    var timestamp: Date?,
    var latitude: Double?,
    var longitude: Double?
) {
    constructor(): this(null, null, null,null, null, null, null, null, null, null)

    constructor(vehicleEntity: VehicleEntity): this(){
        this.id = vehicleEntity.id
        this.type = vehicleEntity.type
        this.model = vehicleEntity.model
        this.manufacturer = vehicleEntity.manufacturer
        this.color = vehicleEntity.color
        this.imageUrl = vehicleEntity.imageUrl
        this.description = vehicleEntity.description
        this.timestamp = vehicleEntity.timestamp
        this.latitude = vehicleEntity.latitude
        this.longitude = vehicleEntity.longitude
    }

    fun toEntity(): VehicleEntity{
        val vehicleEntity = VehicleEntity()

        vehicleEntity.id = id
        vehicleEntity.type = type
        vehicleEntity.model = model
        vehicleEntity.manufacturer = manufacturer
        vehicleEntity.color = color
        vehicleEntity.imageUrl = imageUrl
        vehicleEntity.description = description
        vehicleEntity.timestamp = timestamp
        vehicleEntity.latitude = latitude
        vehicleEntity.longitude = longitude

        return vehicleEntity
    }

    override fun toString(): String {
        return "VehicleEntity(" +
                "id=$id," +
                " type=$type," +
                " model=$model," +
                " manufacturer=$manufacturer," +
                " color=$color," +
                " imageUrl=$imageUrl," +
                " description=$description," +
                " timestamp=$timestamp," +
                " latitude=$latitude," +
                " longitude=$longitude)"
    }
}