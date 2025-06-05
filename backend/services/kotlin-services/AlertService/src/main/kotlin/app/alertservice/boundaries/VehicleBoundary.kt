package app.alertservice.boundaries

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
    var stayDuration: Long?,
    var latitude: Double?,
    var longitude: Double?
) {
    constructor(): this(null, null, null, null,null, null, null, null, null, null, null)

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
                " stayDuration=$stayDuration," +
                " latitude=$latitude," +
                " longitude=$longitude)"
    }
}