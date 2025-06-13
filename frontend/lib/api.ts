
// API Configuration
const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"

// Types matching your backend
export interface VehicleBoundary {
  id: string
  cameraId: string
  type: string
  manufacturer: string
  color: String
  typeProb: number
  manufacturerProb: number
  colorProb: number
  imageUrl: string
  description: string
  timestamp: string
  stayDuration: number
  latitude: number
  longitude: number
}

export interface Alert {
  id: string
  cameraId: string
  type: string
  severity: "Low" | "Medium" | "High" | "Critical"
  description: string
  timestamp: string
  vehicleBoundary: VehicleBoundary
}

export interface Camera {
  id: string
  name: string
  emails: string[]
  location: string
  alertCount: number
  isActive: boolean
  status: "online" | "offline" | "maintenance"
  lastActivity: string
}

export interface CameraSchedule {
  enabled: boolean
  days: string[]
  startTime: string
  endTime: string
}

export interface LoginCredentials {
  email: string
  password: string
}

export interface User {
  id: string
  email: string
  name?: string
  role?: string
}

// API Service Class
class ApiService {
  private async request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
    const url = `${API_BASE_URL}${endpoint}`

    // Get auth token from localStorage (adjust based on your auth strategy)
    const token = localStorage.getItem("authToken")

    const config: RequestInit = {
      headers: {
        "Content-Type": "application/json",
        ...(token && { Authorization: `Bearer ${token}` }),
        ...options.headers,
      },
      ...options,
    }

    try {
      const response = await fetch(url, config)
      if (!response.ok) {
        if (response.status === 401) {
          // Handle unauthorized - redirect to login
          localStorage.removeItem("authToken")
          localStorage.removeItem("user")
          window.location.href = "/"
          throw new Error("Unauthorized")
        }
        throw new Error(`HTTP error! status: ${response.status}`)
      }

      return await response.json()
    } catch (error) {
      console.error("API request failed:", error)
      throw error
    }
  }

  // Authentication
  async login(credentials: LoginCredentials): Promise<{ user: User; token: string }> {
    return this.request<{ user: User; token: string }>("/users/login", {
      method: "POST",
      body: JSON.stringify(credentials),
    })
  }

  async logout(): Promise<void> {
    return this.request("/auth/logout", {
      method: "POST",
    })
  }

  // Camera Management
  async getCameras(email: string): Promise<Camera[]> {
    return this.request(`/cameras/getCamerasByEmail/${email}`)
  }

  async getCamera(id: string): Promise<Camera> {
    return this.request(`/cameras/getCameraById/${id}`)
  }

  async updateCameraStatus(id: string, isActive: boolean): Promise<Camera> {
    return this.request(`/cameras/${id}/status`, {
      method: "PUT",
      body: JSON.stringify({ isActive }),
    })
  }

  async updateCameraSchedule(id: string, schedule: CameraSchedule): Promise<void> {
    return this.request(`/cameras/schedule/${id}`, {
      method: "PUT",
      body: JSON.stringify(schedule),
    })
  }

  async getCameraSchedule(id: string): Promise<CameraSchedule> {
    return this.request(`/cameras/schedule/${id}`)
  }

  // Alerts/Violations
  async getAlerts(
    cameraId: string,
  ): Promise<Alert[]> {
   
    const endpoint = `/alerts/getAlertsByCamera/${cameraId}`

    return this.request(endpoint)
  }

  async getAlert(alertId: string): Promise<Alert> {
    return this.request(`/alerts/${alertId}`)
  }

  // Dashboard Stats
  async getDashboardStats(): Promise<{
    totalCameras: number
    activeCameras: number
    totalAlerts: number
    avgResponseTime: string
  }> {
    return this.request("/dashboard/stats")
  }
}

// Export singleton instance
export const apiService = new ApiService()

// Utility function to handle API errors
export const handleApiError = (error: any) => {
  if (error.message === "Unauthorized") {
    return "Session expired. Please login again."
  }
  if (error.message.includes("Failed to fetch")) {
    return "Unable to connect to server. Please check your connection."
  }
  return error.message || "An unexpected error occurred."
}
