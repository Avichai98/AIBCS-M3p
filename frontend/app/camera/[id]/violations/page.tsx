"use client"

import { useState, useEffect } from "react"
import { useRouter, useParams } from "next/navigation"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Input } from "@/components/ui/input"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { ArrowLeft, Search, AlertTriangle, Clock, MapPin, Car } from "lucide-react"
import Link from "next/link"
import Image from "next/image"
import { apiService } from "@/lib/api"

interface VehicleBoundary {
  id: string
  cameraId: string
  type: string
  manufacturer: string
  color: string
  typeProb: number
  manufacturerProb: number
  colorProb: number
  imageUrl: string
  description: string
  timestamp: string
  stayDuration: number
  stayDurationFormatted: string,
  top: number,
  left: number,
  width: number,
  height: number,
  latitude: number
  longitude: number
}

interface Alert {
  id: string
  cameraId: string
  type: string
  severity: "Low" | "Medium" | "High" | "Critical"
  description: string
  timestamp: string
  vehicleBoundary: VehicleBoundary
}

type DamageDescription = {
  boxes: any[];
  confidences: number[];
  classes: string[];
};

export default function ViolationsPage() {
  const [alerts, setAlerts] = useState<Alert[]>([])
  const [filteredAlerts, setFilteredAlerts] = useState<Alert[]>([])
  const [searchTerm, setSearchTerm] = useState("")
  const [severityFilter, setSeverityFilter] = useState("all")
  const [typeFilter, setTypeFilter] = useState("all")
  const [camera, setCamera] = useState<any>(null)
  const router = useRouter()
  const params = useParams()

  const hasDamage = (description: DamageDescription): boolean => {
    return (
      description.boxes.length > 0 ||
      description.confidences.length > 0 ||
      description.classes.length > 0
    );
  };

  useEffect(() => {
    const loadViolationsData = async () => {
      try {
        // Check authentication
        const userData = localStorage.getItem("user")
        if (!userData) {
          router.push("/")
          return
        }

        // Load camera info and alerts
        const [cameraData, alertsData] = await Promise.all([
          apiService.getCamera(params.id as string),
          apiService.getAlerts(params.id as string),
        ])

        setCamera(cameraData)
        setAlerts(alertsData)
        setFilteredAlerts(alertsData)
      } catch (error) {
        console.error("Failed to load violations data:", error)
        // Handle error
      }
    }

    loadViolationsData()
  }, [params.id, router])

  // Update the filtering effect to also call API when filters change
  useEffect(() => {
    const loadFilteredAlerts = async () => {
      try {
        const alertsData = await apiService.getAlerts(params.id as string)

        setFilteredAlerts(alertsData)
      } catch (error) {
        console.error("Failed to filter alerts:", error)
      }
    }

    if (params.id) {
      loadFilteredAlerts()
    }
  }, [searchTerm, severityFilter, typeFilter, params.id])

  const getSeverityColor = (severity: string) => {
    switch (severity) {
      case "Critical":
        return "destructive"
      case "High":
        return "destructive"
      case "Medium":
        return "default"
      case "Low":
        return "secondary"
      default:
        return "secondary"
    }
  }

  const formatTimestamp = (timestamp: string) => {
    return new Date(timestamp).toLocaleString()
  }

  const uniqueTypes = [...new Set(alerts.map((alert) => alert.type))]

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white shadow-sm border-b">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center h-16">
            <Link href="/dashboard">
              <Button variant="ghost" size="sm" className="mr-4">
                <ArrowLeft className="h-4 w-4 mr-2" />
                Back to Dashboard
              </Button>
            </Link>
            <div>
              <h1 className="text-xl font-semibold text-gray-900">{camera?.name} - Violations</h1>
              <p className="text-sm text-gray-600">{camera?.location}</p>
            </div>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Filters */}
        <Card className="mb-6">
          <CardHeader>
            <CardTitle className="text-lg">Filter Violations</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div className="relative">
                <Search className="absolute left-3 top-3 h-4 w-4 text-gray-400" />
                <Input
                  placeholder="Search violations..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="pl-10"
                />
              </div>
              <Select value={severityFilter} onValueChange={setSeverityFilter}>
                <SelectTrigger>
                  <SelectValue placeholder="Filter by severity" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">All Severities</SelectItem>
                  <SelectItem value="Critical">Critical</SelectItem>
                  <SelectItem value="High">High</SelectItem>
                  <SelectItem value="Medium">Medium</SelectItem>
                  <SelectItem value="Low">Low</SelectItem>
                </SelectContent>
              </Select>
              <Select value={typeFilter} onValueChange={setTypeFilter}>
                <SelectTrigger>
                  <SelectValue placeholder="Filter by type" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">All Types</SelectItem>
                  {uniqueTypes.map((type) => (
                    <SelectItem key={type} value={type}>
                      {type}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </CardContent>
        </Card>

        {/* Stats */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
          <Card>
            <CardContent className="p-4">
              <div className="flex items-center">
                <AlertTriangle className="h-6 w-6 text-red-600 mr-2" />
                <div>
                  <p className="text-sm text-gray-600">Total Alerts</p>
                  <p className="text-xl font-bold">{filteredAlerts.length}</p>
                </div>
              </div>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="p-4">
              <div className="flex items-center">
                <div className="h-6 w-6 bg-red-100 rounded-full flex items-center justify-center mr-2">
                  <div className="h-3 w-3 bg-red-600 rounded-full"></div>
                </div>
                <div>
                  <p className="text-sm text-gray-600">Critical</p>
                  <p className="text-xl font-bold">{filteredAlerts.filter((a) => a.severity === "Critical").length}</p>
                </div>
              </div>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="p-4">
              <div className="flex items-center">
                <div className="h-6 w-6 bg-yellow-100 rounded-full flex items-center justify-center mr-2">
                  <div className="h-3 w-3 bg-yellow-600 rounded-full"></div>
                </div>
                <div>
                  <p className="text-sm text-gray-600">High</p>
                  <p className="text-xl font-bold">{filteredAlerts.filter((a) => a.severity === "High").length}</p>
                </div>
              </div>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="p-4">
              <div className="flex items-center">
                <Clock className="h-6 w-6 text-blue-600 mr-2" />
                <div>
                  <p className="text-sm font-medium">Last Alert</p>
                  <p className="text-sm font-medium">{filteredAlerts.length > 0 ? "2 min ago" : "No alerts"}</p>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Violations List */}
        <div className="space-y-4">
          {filteredAlerts.map((alert) => {
            let parsed: DamageDescription | null = null;
            try {
              parsed = JSON.parse(alert.vehicleBoundary.description);
            } catch {}

            const showDamage = parsed && hasDamage(parsed);

            return (
              <Card key={alert.id} className="hover:shadow-md transition-shadow">
                <CardContent className="p-6">
                  <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
                    {/* Vehicle Image */}
                    <div className="lg:col-span-1">
                      <div className="relative aspect-[4/3] rounded-lg overflow-hidden bg-gray-100">
                        <Image
                          src={alert.vehicleBoundary.imageUrl || "/placeholder.svg"}
                          alt="Vehicle violation"
                          fill
                          className="object-cover"
                        />
                      </div>
                    </div>

                    {/* Alert Details */}
                    <div className="lg:col-span-2 space-y-4">
                      <div className="flex items-start justify-between">
                        <div>
                          <div className="flex items-center space-x-2 mb-2">
                            <Badge variant={getSeverityColor(alert.severity)}>{alert.severity}</Badge>
                            <Badge variant="outline">{alert.type}</Badge>
                          </div>
                          <h3 className="text-lg font-semibold text-gray-900 mb-1">{alert.description}</h3>
                          <p className="text-sm text-gray-600">Alert ID: {alert.id}</p>
                        </div>
                      </div>

                      <div className="grid grid-cols-2 gap-4 text-sm">
                        <div className="flex items-center text-gray-600">
                          <Clock className="h-4 w-4 mr-2" />
                          {formatTimestamp(alert.timestamp)}
                        </div>
                        <div className="flex items-center text-gray-600">
                          <MapPin className="h-4 w-4 mr-2" />
                          {alert.vehicleBoundary.latitude.toFixed(3)}, {alert.vehicleBoundary.longitude.toFixed(3)}
                        </div>
                      </div>
                    </div>

                    {/* Vehicle Details */}
                    <div className="lg:col-span-1 space-y-3">
                      <div className="flex items-center text-gray-700 mb-2">
                        <Car className="h-4 w-4 mr-2" />
                        <span className="font-medium">Vehicle Details</span>
                      </div>
                      <div className="space-y-2 text-sm">
                        <div>
                          <span className="text-gray-600">Type:</span>
                          <span className="ml-2 font-medium">{alert.vehicleBoundary.type}</span>
                        </div>
                        <div>
                          <span className="text-gray-600">Manufacturer:</span>
                          <span className="ml-2 font-medium">{alert.vehicleBoundary.manufacturer}</span>
                        </div>
                        <div>
                          <span className="text-gray-600">Color:</span>
                          <span className="ml-2 font-medium">{alert.vehicleBoundary.color}</span>
                        </div>
                        {/* Conditionally show damage info only if it exists */}
                        {showDamage && (
                          <div>
                            <span className="text-gray-600">Damage:</span>
                            <span className="ml-2 font-medium">{JSON.stringify(parsed)}</span>
                          </div>
                        )}
                        {alert.vehicleBoundary.stayDuration > 0 && (
                          <div>
                            <span className="text-gray-600">Duration:</span>
                            <span className="ml-2 font-medium">{alert.vehicleBoundary.stayDurationFormatted} </span>
                          </div>
                        )}
                      </div>
                    </div>
                  </div>
                </CardContent>
              </Card>
            )
          })}
        </div>

        {filteredAlerts.length === 0 && (
          <Card>
            <CardContent className="p-12 text-center">
              <AlertTriangle className="h-12 w-12 text-gray-400 mx-auto mb-4" />
              <h3 className="text-lg font-medium text-gray-900 mb-2">No violations found</h3>
              <p className="text-gray-600">
                {searchTerm || severityFilter !== "all" || typeFilter !== "all"
                  ? "Try adjusting your filters to see more results."
                  : "This camera hasn't detected any violations yet."}
              </p>
            </CardContent>
          </Card>
        )}
      </main>
    </div>
  )
}
