package app.dataservice.entities

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.util.*

@Document (collection = "vehicles")
class VehicleEntity (
    @Id var id: String?,
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