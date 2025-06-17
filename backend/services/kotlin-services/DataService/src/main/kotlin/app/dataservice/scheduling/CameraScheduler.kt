package app.dataservice.scheduling

import app.dataservice.boundaries.CameraSchedule
import app.dataservice.interfaces.CameraService
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.time.*
import java.util.Date
import java.util.concurrent.ScheduledFuture

@Service
class CameraScheduler(
    private val cameraService: CameraService
) {
    private val scheduler = ThreadPoolTaskScheduler().apply { initialize() }
    private val tasks = mutableMapOf<String, ScheduledFuture<*>>()

    lateinit var dataServiceUrl: String
    lateinit var webClient: WebClient

    @Value("\${remote.python.service.url: http://car-detection-service:5000}")
    fun setRemoteUrl(url: String) {
        this.dataServiceUrl = url
    }

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
                activateCamera(cameraId).subscribe()
            }, Date.from(startInstant))

            val stopTask = scheduler.schedule({
                println("⏹️ Stopping camera $cameraId at ${Date.from(endInstant)}")
                deactivateCamera(cameraId).subscribe()
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

    fun activateCamera(cameraId: String): Mono<Void> {
        println("🚀 Camera $cameraId ACTIVATED")

        return webClient
            .get()
            .uri("/build")
            .retrieve()
            .bodyToMono(String::class.java)
            .doOnSuccess { println("📸 Camera $cameraId build response: $it") }
            .then(
                webClient.post()
                    .uri("/start")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(String::class.java)
                    .doOnSuccess { println("📸 Camera $cameraId start response: $it") }
                    .doOnError { e -> println("❌ Error starting camera $cameraId: ${e.message}") }
            )
            .then()
    }

    fun deactivateCamera(cameraId: String): Mono<Void> {
        println("🛑 Camera $cameraId DEACTIVATED")

        return webClient.post()
            .uri("/stop")
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(String::class.java)
            .doOnSuccess { println("📸 Camera $cameraId stop response: $it") }
            .doOnError { e -> println("❌ Error stopping camera $cameraId: ${e.message}") }
            .then()
    }
}
