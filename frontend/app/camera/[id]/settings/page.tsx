"use client"

import { useState, useEffect } from "react"
import { useRouter, useParams } from "next/navigation"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Switch } from "@/components/ui/switch"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { ArrowLeft, Clock, Calendar, Save, AlertCircle } from "lucide-react"
import Link from "next/link"
import { Alert, AlertDescription } from "@/components/ui/alert"
import { apiService } from "@/lib/api"

interface CameraSchedule {
  enabled: boolean
  days: string[]
  startTime: string
  endTime: string
}

interface CameraSettings {
  id: string
  name: string
  location: string
  isActive: boolean
  schedule: CameraSchedule
}

export default function CameraSettingsPage() {
  const [settings, setSettings] = useState<CameraSettings | null>(null)
  const [hasChanges, setHasChanges] = useState(false)
  const [saveSuccess, setSaveSuccess] = useState(false)
  const router = useRouter()
  const params = useParams()

  const daysOfWeek = [
    { value: "monday", label: "Monday" },
    { value: "tuesday", label: "Tuesday" },
    { value: "wednesday", label: "Wednesday" },
    { value: "thursday", label: "Thursday" },
    { value: "friday", label: "Friday" },
    { value: "saturday", label: "Saturday" },
    { value: "sunday", label: "Sunday" },
  ]

  const timeOptions = Array.from({ length: 24 }, (_, i) => {
    const hour = i.toString().padStart(2, "0")
    return { value: `${hour}:00`, label: `${hour}:00` }
  })

  useEffect(() => {
    const loadCameraSettings = async () => {
      try {
        // Check authentication
        const userData = localStorage.getItem("user")
        if (!userData) {
          router.push("/")
          return
        }

        // Load camera data and schedule
        const [cameraData, scheduleData] = await Promise.all([
          apiService.getCamera(params.id as string),
          apiService.getCameraSchedule(params.id as string),
        ])

        setSettings({
          id: cameraData.id,
          name: cameraData.name,
          location: cameraData.location,
          isActive: cameraData.isActive,
          schedule: scheduleData,
        })
      } catch (error) {
        console.error("Failed to load camera settings:", error)
      }
    }

    loadCameraSettings()
  }, [params.id, router])

  const getCameraName = (id: string) => {
    const names: { [key: string]: string } = {
      CAM001: "Main Entrance",
      CAM002: "Parking Lot A",
      CAM003: "Loading Dock",
      CAM004: "Visitor Parking",
    }
    return names[id] || "Unknown Camera"
  }

  const getCameraLocation = (id: string) => {
    const locations: { [key: string]: string } = {
      CAM001: "Building A - Front Gate",
      CAM002: "North Parking Area",
      CAM003: "Building B - Rear",
      CAM004: "South Entrance",
    }
    return locations[id] || "Unknown Location"
  }

  const handleToggleActive = () => {
    if (!settings) return
    setSettings((prev) => (prev ? { ...prev, isActive: !prev.isActive } : null))
    setHasChanges(true)
  }

  const handleScheduleToggle = () => {
    if (!settings) return
    setSettings((prev) =>
      prev
        ? {
            ...prev,
            schedule: { ...prev.schedule, enabled: !prev.schedule.enabled },
          }
        : null,
    )
    setHasChanges(true)
  }

  const handleDayToggle = (day: string) => {
    if (!settings) return
    const currentDays = settings.schedule.days
    const newDays = currentDays.includes(day) ? currentDays.filter((d) => d !== day) : [...currentDays, day]

    setSettings((prev) =>
      prev
        ? {
            ...prev,
            schedule: { ...prev.schedule, days: newDays },
          }
        : null,
    )
    setHasChanges(true)
  }

  const handleTimeChange = (type: "startTime" | "endTime", value: string) => {
    if (!settings) return
    setSettings((prev) =>
      prev
        ? {
            ...prev,
            schedule: { ...prev.schedule, [type]: value },
          }
        : null,
    )
    setHasChanges(true)
  }

  const handleSave = async () => {
    if (!settings) return

    try {
      // Save camera status and schedule
      await Promise.all([
        apiService.updateCameraStatus(settings.id, settings.isActive),
        apiService.updateCameraSchedule(settings.id, settings.schedule),
      ])

      setHasChanges(false)
      setSaveSuccess(true)
      setTimeout(() => setSaveSuccess(false), 3000)
    } catch (error) {
      console.error("Failed to save settings:", error)
      // Handle error - show error message
    }
  }

  const formatDaysDisplay = () => {
    if (!settings) return ""
    const { days } = settings.schedule
    if (days.length === 7) return "Every day"
    if (days.length === 5 && !days.includes("saturday") && !days.includes("sunday")) {
      return "Weekdays"
    }
    if (days.length === 2 && days.includes("saturday") && days.includes("sunday")) {
      return "Weekends"
    }
    return days.map((day) => day.charAt(0).toUpperCase() + day.slice(1)).join(", ")
  }

  if (!settings) return null

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white shadow-sm border-b">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-16">
            <div className="flex items-center">
              <Link href="/dashboard">
                <Button variant="ghost" size="sm" className="mr-4">
                  <ArrowLeft className="h-4 w-4 mr-2" />
                  Back to Dashboard
                </Button>
              </Link>
              <div>
                <h1 className="text-xl font-semibold text-gray-900">{settings.name} - Settings</h1>
                <p className="text-sm text-gray-600">{settings.location}</p>
              </div>
            </div>
            {hasChanges && (
              <Button onClick={handleSave}>
                <Save className="h-4 w-4 mr-2" />
                Save Changes
              </Button>
            )}
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {saveSuccess && (
          <Alert className="mb-6">
            <AlertCircle className="h-4 w-4" />
            <AlertDescription>Camera settings have been saved successfully.</AlertDescription>
          </Alert>
        )}

        <div className="space-y-6">
          {/* Camera Status */}
          <Card>
            <CardHeader>
              <CardTitle>Camera Status</CardTitle>
              <CardDescription>Control whether this camera is actively monitoring for violations</CardDescription>
            </CardHeader>
            <CardContent>
              <div className="flex items-center justify-between">
                <div>
                  <Label htmlFor="camera-active" className="text-base font-medium">
                    Camera Active
                  </Label>
                  <p className="text-sm text-gray-600 mt-1">
                    {settings.isActive
                      ? "Camera is currently monitoring and detecting violations"
                      : "Camera is stopped and not monitoring"}
                  </p>
                </div>
                <Switch id="camera-active" checked={settings.isActive} onCheckedChange={handleToggleActive} />
              </div>
            </CardContent>
          </Card>

          {/* Schedule Settings */}
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center">
                <Clock className="h-5 w-5 mr-2" />
                Active Schedule
              </CardTitle>
              <CardDescription>Define when this camera should be actively monitoring</CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              {/* Enable Schedule */}
              <div className="flex items-center justify-between">
                <div>
                  <Label htmlFor="schedule-enabled" className="text-base font-medium">
                    Enable Schedule
                  </Label>
                  <p className="text-sm text-gray-600 mt-1">
                    {settings.schedule.enabled ? "Camera follows the schedule below" : "Camera runs 24/7 when active"}
                  </p>
                </div>
                <Switch
                  id="schedule-enabled"
                  checked={settings.schedule.enabled}
                  onCheckedChange={handleScheduleToggle}
                />
              </div>

              {settings.schedule.enabled && (
                <>
                  {/* Days Selection */}
                  <div>
                    <Label className="text-base font-medium mb-3 block">
                      <Calendar className="h-4 w-4 inline mr-2" />
                      Active Days
                    </Label>
                    <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
                      {daysOfWeek.map((day) => (
                        <div key={day.value} className="flex items-center space-x-2">
                          <Switch
                            id={day.value}
                            checked={settings.schedule.days.includes(day.value)}
                            onCheckedChange={() => handleDayToggle(day.value)}
                          />
                          <Label htmlFor={day.value} className="text-sm">
                            {day.label}
                          </Label>
                        </div>
                      ))}
                    </div>
                  </div>

                  {/* Time Range */}
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div>
                      <Label className="text-base font-medium mb-2 block">Start Time</Label>
                      <Select
                        value={settings.schedule.startTime}
                        onValueChange={(value) => handleTimeChange("startTime", value)}
                      >
                        <SelectTrigger>
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          {timeOptions.map((time) => (
                            <SelectItem key={time.value} value={time.value}>
                              {time.label}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    </div>
                    <div>
                      <Label className="text-base font-medium mb-2 block">End Time</Label>
                      <Select
                        value={settings.schedule.endTime}
                        onValueChange={(value) => handleTimeChange("endTime", value)}
                      >
                        <SelectTrigger>
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          {timeOptions.map((time) => (
                            <SelectItem key={time.value} value={time.value}>
                              {time.label}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    </div>
                  </div>

                  {/* Schedule Summary */}
                  <div className="bg-blue-50 p-4 rounded-lg">
                    <h4 className="font-medium text-blue-900 mb-2">Current Schedule</h4>
                    <p className="text-blue-800 text-sm">
                      Camera will be active on <strong>{formatDaysDisplay()}</strong> from{" "}
                      <strong>{settings.schedule.startTime}</strong> to <strong>{settings.schedule.endTime}</strong>
                    </p>
                  </div>
                </>
              )}
            </CardContent>
          </Card>

          {/* Camera Information */}
          <Card>
            <CardHeader>
              <CardTitle>Camera Information</CardTitle>
              <CardDescription>Basic information about this camera</CardDescription>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
                <div>
                  <Label className="font-medium text-gray-700">Camera ID</Label>
                  <p className="text-gray-900 mt-1">{settings.id}</p>
                </div>
                <div>
                  <Label className="font-medium text-gray-700">Camera Name</Label>
                  <p className="text-gray-900 mt-1">{settings.name}</p>
                </div>
                <div>
                  <Label className="font-medium text-gray-700">Location</Label>
                  <p className="text-gray-900 mt-1">{settings.location}</p>
                </div>
                <div>
                  <Label className="font-medium text-gray-700">Status</Label>
                  <p className={`mt-1 font-medium ${settings.isActive ? "text-green-600" : "text-red-600"}`}>
                    {settings.isActive ? "Active" : "Inactive"}
                  </p>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      </main>
    </div>
  )
}
