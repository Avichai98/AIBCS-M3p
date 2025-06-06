package app.alertservice.boundaries

import java.util.Date

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
    var timestamp: Date?,
    var stayDuration: Long?,
    var latitude: Double?,
    var longitude: Double?
) {
    constructor() : this(null, null, null, null, null, null, null, null, null, null, null, null, null, null)

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
                " latitude=$latitude," +
                " longitude=$longitude)"
    }
}