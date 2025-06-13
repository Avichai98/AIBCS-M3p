"use client"

import { useState, useEffect } from "react"
import { useRouter } from "next/navigation"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Switch } from "@/components/ui/switch"
import { Camera, AlertTriangle, Clock, Settings, LogOut, Play, Pause } from "lucide-react"
import Link from "next/link"
import { apiService } from "@/lib/api"

interface CameraType {
  id: string
  name: string
  emails: string[]
  location: string
  alertCount: number
  isActive: boolean
  status: "online" | "offline" | "maintenance"
  lastActivity: string
}

export default function Dashboard() {
  const [cameras, setCameras] = useState<CameraType[]>([])
  const [user, setUser] = useState<any>(null)
  const router = useRouter()

  useEffect(() => {
    const loadDashboardData = async () => {
      try {
        // Check authentication
        const userData = localStorage.getItem("user")
        console.log("📦 User data from localStorage:", userData)
        if (!userData) {
          router.push("/")
          return
        }
        setUser(JSON.parse(userData))

        // Load cameras and stats from backend
        const [camerasData, /*statsData*/] = await Promise.all([apiService.getCameras(JSON.parse(userData).email), /*apiService.getDashboardStats()*/])
        
        console.log("📷 Fetched cameras data:", camerasData)

        setCameras(camerasData)
        // You can use statsData for the overview cards if needed
      } catch (error) {
        console.error("Failed to load dashboard data:", error)
        // Handle error - maybe show a toast notification
      }
    }

    loadDashboardData()
  }, [router])

  const handleLogout = () => {
    localStorage.removeItem("user")
    router.push("/")
  }

  const toggleCamera = async (cameraId: string) => {
  try {
    const camera = cameras.find((c) => c.id === cameraId)
    if (!camera) return

    await apiService.updateCameraStatus(cameraId, !camera.isActive)

    // Optimistically update the camera state
    setCameras((prev) =>
      prev.map((cam) =>
        cam.id === cameraId ? { ...cam, isActive: !cam.isActive } : cam
      )
    )
  } catch (error) {
    console.error("Failed to toggle camera:", error)
    // Handle error - maybe show a toast notification
  }
}


  const getStatusColor = (status: string) => {
    switch (status) {
      case "online":
        return "bg-green-500"
      case "offline":
        return "bg-red-500"
      case "maintenance":
        return "bg-yellow-500"
      default:
        return "bg-gray-500"
    }
  }

  if (!user) return null

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white shadow-sm border-b">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center h-16">
            <div className="flex items-center">
              <Camera className="h-8 w-8 text-blue-600 mr-3" />
              <h1 className="text-xl font-semibold text-gray-900">Smart Violation Monitor</h1>
            </div>
            <div className="flex items-center space-x-4">
              <span className="text-sm text-gray-600">Welcome, {user.email}</span>
              <Button variant="outline" size="sm" onClick={handleLogout}>
                <LogOut className="h-4 w-4 mr-2" />
                Logout
              </Button>
            </div>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="mb-8">
          <h2 className="text-2xl font-bold text-gray-900 mb-2">Camera Dashboard</h2>
          <p className="text-gray-600">Monitor your cameras and view violation alerts</p>
        </div>

        {/* Stats Overview */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
          <Card>
            <CardContent className="p-6">
              <div className="flex items-center">
                <Camera className="h-8 w-8 text-blue-600" />
                <div className="ml-4">
                  <p className="text-sm font-medium text-gray-600">Total Cameras</p>
                  <p className="text-2xl font-bold text-gray-900">{cameras.length}</p>
                </div>
              </div>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="p-6">
              <div className="flex items-center">
                <div className="h-8 w-8 bg-green-100 rounded-full flex items-center justify-center">
                  <div className="h-3 w-3 bg-green-500 rounded-full"></div>
                </div>
                <div className="ml-4">
                  <p className="text-sm font-medium text-gray-600">Active Cameras</p>
                  <p className="text-2xl font-bold text-gray-900">{cameras.filter((c) => c.isActive).length}</p>
                </div>
              </div>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="p-6">
              <div className="flex items-center">
                <AlertTriangle className="h-8 w-8 text-red-600" />
                <div className="ml-4">
                  <p className="text-sm font-medium text-gray-600">Total Alerts</p>
                  <p className="text-2xl font-bold text-gray-900">
                    {cameras.reduce((sum, cam) => sum + cam.alertCount, 0)}
                  </p>
                </div>
              </div>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="p-6">
              <div className="flex items-center">
                <Clock className="h-8 w-8 text-yellow-600" />
                <div className="ml-4">
                  <p className="text-sm font-medium text-gray-600">Avg Response</p>
                  <p className="text-2xl font-bold text-gray-900">2.3m</p>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Camera Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {cameras.map((camera) => (
            <Card key={camera.id} className="hover:shadow-lg transition-shadow">
              <CardHeader className="pb-3">
                <div className="flex items-center justify-between">
                  <div className="flex items-center">
                    <div className={`h-3 w-3 rounded-full ${getStatusColor(camera.status)} mr-2`}></div>
                    <CardTitle className="text-lg">{camera.name}</CardTitle>
                  </div>
                  <Badge variant={camera.status === "online" ? "default" : "secondary"}>{camera.status}</Badge>
                </div>
                <CardDescription>{camera.location}</CardDescription>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-600">Alerts Today</span>
                    <Badge variant={camera.alertCount > 10 ? "destructive" : "secondary"}>{camera.alertCount}</Badge>
                  </div>

                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-600">Camera Active</span>
                    <div className="flex items-center space-x-2">
                      <Switch checked={camera.isActive} onCheckedChange={() => toggleCamera(camera.id)} />
                      {camera.isActive ? (
                        <Play className="h-4 w-4 text-green-600" />
                      ) : (
                        <Pause className="h-4 w-4 text-red-600" />
                      )}
                    </div>
                  </div>

                  <div className="text-xs text-gray-500">Last activity: {camera.lastActivity}</div>

                  <div className="flex space-x-2 pt-2">
                    <Link href={`/camera/${camera.id}/violations`} className="flex-1">
                      <Button variant="outline" size="sm" className="w-full">
                        View Alerts
                      </Button>
                    </Link>
                    <Link href={`/camera/${camera.id}/settings`}>
                      <Button variant="outline" size="sm">
                        <Settings className="h-4 w-4" />
                      </Button>
                    </Link>
                  </div>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      </main>
    </div>
  )
}
