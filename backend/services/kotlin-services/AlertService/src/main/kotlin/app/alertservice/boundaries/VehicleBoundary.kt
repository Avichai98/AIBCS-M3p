package app.alertservice.boundaries

import java.time.LocalDateTime

class VehicleBoundary(
    var id: String?,
    var cameraId: String?,
    var type: String?,
    var manufacturer: String?,
    var color: String?,
    var typeProb: Double?,
    var manufacturerProb: Double?,
    var colorProb: Double?,
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
    constructor() : this(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null)

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