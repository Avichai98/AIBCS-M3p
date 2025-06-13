package app.dataservice.boundaries

class CameraSchedule(
    var enabled: Boolean?,
    var days: List<String>?,
    var startTime: String?,
    var endTime: String?
)
{
    constructor() : this(null, null, null, null)

    override fun toString(): String {
        return "CameraSchedule(" +
                "enabled=$enabled," +
                " days=$days," +
                " startTime=$startTime," +
                " endTime=$endTime" +
                ")"
    }
}
