// API Configuration
const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api"

// Types matching your backend
export interface VehicleBoundary {
  id: string
  type: string
  model: string
  manufacturer: string
  color: string
  imageUrl: string
  description: string
  timestamp: string
  stayDuration: number
  latitude: number
  longitude: number
}

export interface Alert {
  id: string
  type: string
  severity: "Low" | "Medium" | "High" | "Critical"
  description: string
  timestamp: string
  cameraId?: string // You mentioned this will be added later
  vehicleBoundary: VehicleBoundary
}

export interface Camera {
  id: string
  name: string
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
    return this.request("/auth/login", {
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
  async getCameras(): Promise<Camera[]> {
    return this.request("/cameras")
  }

  async getCamera(id: string): Promise<Camera> {
    return this.request(`/cameras/${id}`)
  }

  async updateCameraStatus(id: string, isActive: boolean): Promise<Camera> {
    return this.request(`/cameras/${id}/status`, {
      method: "PUT",
      body: JSON.stringify({ isActive }),
    })
  }

  async updateCameraSchedule(id: string, schedule: CameraSchedule): Promise<void> {
    return this.request(`/cameras/${id}/schedule`, {
      method: "PUT",
      body: JSON.stringify(schedule),
    })
  }

  async getCameraSchedule(id: string): Promise<CameraSchedule> {
    return this.request(`/cameras/${id}/schedule`)
  }

  // Alerts/Violations
  async getAlerts(
    cameraId: string,
    filters?: {
      severity?: string
      type?: string
      search?: string
      page?: number
      limit?: number
    },
  ): Promise<{ alerts: Alert[]; total: number; page: number; totalPages: number }> {
    const params = new URLSearchParams()
    if (filters?.severity) params.append("severity", filters.severity)
    if (filters?.type) params.append("type", filters.type)
    if (filters?.search) params.append("search", filters.search)
    if (filters?.page) params.append("page", filters.page.toString())
    if (filters?.limit) params.append("limit", filters.limit.toString())

    const queryString = params.toString()
    const endpoint = `/cameras/${cameraId}/alerts${queryString ? `?${queryString}` : ""}`

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
