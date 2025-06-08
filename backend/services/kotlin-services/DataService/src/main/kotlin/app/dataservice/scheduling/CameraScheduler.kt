package app.dataservice.scheduling

import app.dataservice.boundaries.CameraSchedule
import app.dataservice.interfaces.CameraService
import jakarta.annotation.PostConstruct
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.stereotype.Service
import java.time.*
import java.util.Date
import java.util.concurrent.ScheduledFuture

@Service
class CameraScheduler(
    private val cameraService: CameraService
) {

    private val scheduler = ThreadPoolTaskScheduler().apply { initialize() }
    private val tasks = mutableMapOf<String, ScheduledFuture<*>>()

    @PostConstruct
    fun initSchedules() {
        println("📅 Initializing camera schedules from DB")
        cameraService.getAllCameraSchedules()
            .doOnNext { (cameraId, schedule) ->
                println("🗓️ Loading schedule for camera: $cameraId")
                scheduleCamera(cameraId, schedule)
            }
            .subscribe()
    }

    fun scheduleCamera(cameraId: String, schedule: CameraSchedule) {
        cancelExistingSchedule(cameraId)

        val startTime = try {
            schedule.startTime?.let { LocalTime.parse(it) }
        } catch (e: Exception) {
            println("⚠️ Invalid startTime format for camera $cameraId: ${schedule.startTime}")
            null
        }

        val endTime = try {
            schedule.endTime?.let { LocalTime.parse(it) }
        } catch (e: Exception) {
            println("⚠️ Invalid endTime format for camera $cameraId: ${schedule.endTime}")
            null
        }

        val today = LocalDate.now()
        val dayOfWeek = today.dayOfWeek.name // e.g., "SUNDAY"
        val validDays = schedule.days?.map { it.uppercase() }

        println("📅 Checking schedule for camera $cameraId on $dayOfWeek")
        println("✔ Enabled: ${schedule.enabled}, Days: ${validDays}, Start: $startTime, End: $endTime")

        if (schedule.enabled == true && validDays?.contains(dayOfWeek) == true && startTime != null && endTime != null) {
            val zoneId = ZoneId.systemDefault()
            val startInstant = startTime.atDate(today).atZone(zoneId).toInstant()
            val endInstant = endTime.atDate(today).atZone(zoneId).toInstant()

            val startTask = scheduler.schedule({
                println("▶️ Starting camera $cameraId at ${Date.from(startInstant)}")
                activateCamera(cameraId)
            }, Date.from(startInstant))

            val stopTask = scheduler.schedule({
                println("⏹️ Stopping camera $cameraId at ${Date.from(endInstant)}")
                deactivateCamera(cameraId)
            }, Date.from(endInstant))

            tasks[cameraId] = startTask
            tasks["stop-$cameraId"] = stopTask
        } else {
            println("❌ Schedule for camera $cameraId is invalid or not active today.")
        }
    }



    private fun cancelExistingSchedule(cameraId: String) {
        tasks[cameraId]?.cancel(false)
        tasks["stop-$cameraId"]?.cancel(false)
        tasks.remove(cameraId)
        tasks.remove("stop-$cameraId")
    }

    fun activateCamera(cameraId: String) {
        println("🚀 Camera $cameraId ACTIVATED")
        // TODO: קריאה ל־AI-Service (או משהו אחר)
    }

    fun deactivateCamera(cameraId: String) {
        println("🛑 Camera $cameraId DEACTIVATED")
        // TODO: קריאה ל־AI-Service (או משהו אחר)
    }
}
