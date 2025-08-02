package app.dataservice.controllers

import app.dataservice.boundaries.VehicleBoundary
import app.dataservice.interfaces.VehicleService
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/vehicles")
class VehicleController(
    private val vehicleService: VehicleService
) {
    @PostMapping(
        path = ["/create"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    fun create(
        @RequestBody vehicle: VehicleBoundary
    ): Mono<VehicleBoundary>{
        return this.vehicleService
            .createVehicle(vehicle)
    }

    @PutMapping(
        path = ["/update/{id}"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    fun update(
        @PathVariable id: String,
        @RequestBody vehicle: VehicleBoundary
    ): Mono<VehicleBoundary>{
     return this.vehicleService
         .updateVehicle(id, vehicle)
    }

    @GetMapping(
        path = ["/getVehicleById/{id}"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    fun getVehicleById(
        @PathVariable id: String
    ): Mono<VehicleBoundary>{
        return this.vehicleService
            .getVehicleById(id)
    }

    @GetMapping(
        path = ["/getVehicles"],
        produces = [MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_EVENT_STREAM_VALUE]
    )
    @PreAuthorize("hasRole('ADMIN')")
    fun getVehicles(
        @RequestParam(name = "page", required = false, defaultValue = "0") page: Int,
        @RequestParam(name = "size", required = false, defaultValue = "20") size: Int
    ): Flux<VehicleBoundary>{
        return this.vehicleService
            .getVehiclesPage(page, size)
    }

    @GetMapping(
        path = ["/getVehiclesByCameraId/{cameraId}"],
        produces = [MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_EVENT_STREAM_VALUE]
    )
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    fun getVehiclesByCameraId(
        @PathVariable cameraId: String,
        @RequestParam(name = "page", required = false, defaultValue = "0") page: Int,
        @RequestParam(name = "size", required = false, defaultValue = "20") size: Int
    ): Flux<VehicleBoundary>{
        return this.vehicleService
            .getVehiclesByCameraId(cameraId, page, size)
    }

    @GetMapping(
        path = ["/getVehiclesByManufacturer/{manufacturer}"],
        produces = [MediaType.TEXT_EVENT_STREAM_VALUE]
    )
    @PreAuthorize("hasRole('ADMIN')")
    fun getVehiclesByManufacturer(
        @PathVariable manufacturer: String,
        @RequestParam(name = "page", required = false, defaultValue = "0") page: Int,
        @RequestParam(name = "size", required = false, defaultValue = "20") size: Int
    ): Flux<VehicleBoundary>{
        return this.vehicleService
            .getVehiclesByManufacturer(manufacturer, page, size)
    }

    @GetMapping(
        path = ["/getVehiclesByLatitudeAndLongitude/{latitude}/{longitude}"],
        produces = [MediaType.TEXT_EVENT_STREAM_VALUE]
    )
    @PreAuthorize("hasRole('ADMIN')")
    fun getVehiclesByLatitudeAndLongitude(
        @PathVariable latitude: Double,
        @PathVariable longitude: Double,
        @RequestParam(name = "page", required = false, defaultValue = "0") page: Int,
        @RequestParam(name = "size", required = false, defaultValue = "20") size: Int
    ): Flux<VehicleBoundary>{
        return this.vehicleService
            .getVehiclesByLatitudeAndLongitude(latitude, longitude, page, size)
    }

    @GetMapping(
        path = ["/getVehiclesByTimestamp/{timestampStr}"],
        produces = [MediaType.TEXT_EVENT_STREAM_VALUE]
    )
    @PreAuthorize("hasRole('ADMIN')")
    fun getVehiclesByTimestamp(
        @PathVariable("timestampStr") timestampStr: String,
        @RequestParam(name = "page", required = false, defaultValue = "0") page: Int,
        @RequestParam(name = "size", required = false, defaultValue = "20") size: Int
    ): Flux<VehicleBoundary>{
        return this.vehicleService
            .getVehiclesByTimestampAfter(timestampStr, page, size)
    }

    @DeleteMapping(
        path = ["/delete/{id}"]
    )
    @PreAuthorize("hasRole('ADMIN')")
    fun delete(@PathVariable id: String
    ): Mono<Void> {
        return this.vehicleService
            .deleteVehicle(id)
    }

    @DeleteMapping(
        path = ["/deleteAllVehicles"]
    )
    @PreAuthorize("hasRole('ADMIN')")
    fun deleteAllVehicles(): Mono<Void>{
        return this.vehicleService
            .deleteAll()
    }
}