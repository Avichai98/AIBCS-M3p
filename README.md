## AI Based Camera System – Smart Parking Management (AIBCS-M3P)

# By: Tomer Harel and Avichi \_\_

An AI-powered system designed to monitor and manage short-term public parking zones in cities—without relying on license plates or personal identifiers. Our system balances effective enforcement with strict privacy protection, offering municipalities a smarter way to manage urban parking.

## How It’s Made:

**Tech used:**

_Backend:_ Python (vehicle recognition), Kotlin (data & alert services)

_Frontend:_ React.js (user interface)

_Database:_ MongoDB (NoSQL storage for vehicle/alert data)

_Infrastructure:_ Docker Compose (microservices deployment, orchestration)

_AI Models:_ Spectrico (vehicle type/color recognition), suryaremanan Damaged Car parts prediction(vehicle damages), arnabdhar Face Detection (face blurring), morsetechlab license plate detection(blurring license plate)

**System Design:**

Vehicle Recognition Service: Identifies vehicles by visual features (type, brand, color, damages,location) while anonymizing license plates and faces. [process_image]

Comparison Service: Matches vehicles across time intervals to track how long they’ve been parked.[compare_all_vehicles_from_db]

Alert Service: Generates and sends automated alerts/reports when vehicles overstay.

Data Service: Stores vehicle metadata and event logs in MongoDB.

Client Portal: A React-based interface for municipalities to view reports, alerts, and statistics.

## Optimizations

## Lessons Learned

Training and tuning multiple AI models (YOLO vs. Faster R-CNN) taught us that speed matters as much as accuracy—YOLO was chosen for real-time feasibility.

Building in privacy-preserving AI required creative trade-offs, like balancing detection accuracy with anonymization.

Designing the system as microservices early saved time later when adding new features (e.g., additional alert types).

We learned the importance of clear success metrics: response times, recognition accuracy, and usability scores guided development.

## Examples

Here are some comparable projects we looked at for inspiration:

Spectrico Vehicle Recognition: Commercial model for car type & color detection

Parquery Smart Parking: Cloud-based AI parking analysis

DataFromSky FLOW: Real-time traffic analytics with deep learning
