package app.dataservice.boundaries

import app.dataservice.entities.VehicleEntity
import java.time.LocalDateTime

class VehicleBoundary(
    var id: String?,
    var cameraId: String?,
    var type: String?,
    var manufacturer: String?,
    var color: String?,
    var typeProb: Float?,
    var manufacturerProb: Float?,
    var colorProb: Float?,
    var imageUrl: String?,
    var description: String?,
    var timestamp: LocalDateTime?,
    var stayDuration: Long?, // in seconds
    var stayDurationFormatted: String?,
    var top: Int?,
    var left: Int?,
    var width: Int?,
    var height: Int?,
    var latitude: Float?,
    var longitude: Float?
) {
    constructor(): this(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null)

    constructor(vehicleEntity: VehicleEntity): this(){
        this.id = vehicleEntity.id
        this.cameraId = vehicleEntity.cameraId
        this.type = vehicleEntity.type
        this.manufacturer = vehicleEntity.manufacturer
        this.color = vehicleEntity.color
        this.typeProb = vehicleEntity.typeProb
        this.manufacturerProb = vehicleEntity.manufacturerProb
        this.colorProb = vehicleEntity.colorProb
        this.imageUrl = vehicleEntity.imageUrl
        this.description = vehicleEntity.description
        this.timestamp = vehicleEntity.timestamp
        this.stayDuration = vehicleEntity.stayDuration
        this.stayDurationFormatted = vehicleEntity.stayDurationFormatted
        this.top = vehicleEntity.top
        this.left = vehicleEntity.left
        this.width = vehicleEntity.width
        this.height = vehicleEntity.height
        this.latitude = vehicleEntity.latitude
        this.longitude = vehicleEntity.longitude
    }

    fun toEntity(): VehicleEntity{
        val vehicleEntity = VehicleEntity()

        vehicleEntity.id = id
        vehicleEntity.cameraId = cameraId
        vehicleEntity.type = type
        vehicleEntity.manufacturer = manufacturer
        vehicleEntity.color = color
        vehicleEntity.typeProb = typeProb
        vehicleEntity.manufacturerProb = manufacturerProb
        vehicleEntity.colorProb = colorProb
        vehicleEntity.imageUrl = imageUrl
        vehicleEntity.description = description
        vehicleEntity.timestamp = timestamp
        vehicleEntity.stayDuration = stayDuration
        vehicleEntity.stayDurationFormatted = stayDurationFormatted
        vehicleEntity.top = top
        vehicleEntity.left = left
        vehicleEntity.width = width
        vehicleEntity.height = height
        vehicleEntity.latitude = latitude
        vehicleEntity.longitude = longitude

        return vehicleEntity
    }

    override fun toString(): String {
        return "VehicleEntity(" +
                "id=$id," +
                " cameraId=$cameraId," +
                " type=$type," +
                " manufacturer=$manufacturer," +
                " color=$color," +
                " typeProb=$typeProb," +
                " manufacturerProb=$manufacturerProb," +
                " colorProb=$colorProb," +
                " imageUrl=$imageUrl," +
                " description=$description," +
                " timestamp=$timestamp," +
                " stayDuration=$stayDuration," +
                " stayDurationFormatted=$stayDurationFormatted," +
                " top=$top," +
                " left=$left," +
                " width=$width," +
                " height=$height," +
                " latitude=$latitude," +
                " longitude=$longitude)"
    }
}