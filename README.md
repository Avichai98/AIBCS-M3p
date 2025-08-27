# AI Based Camera System – Smart Parking Management (AIBCS-M3P)

## Abstract
This project introduces an **AI-based intelligent camera system** designed to manage public parking in short-term zones while **strictly preserving citizen privacy**. Traditional approaches rely on License Plate Recognition (LPR), which often raise privacy concerns. Our system instead leverages **deep learning models** to recognize vehicles by **visual characteristics** such as type, manufacturer, color, and body damages, while faces and license plates are blurred automatically.  
The result is a scalable, privacy-compliant solution that enables municipalities to enforce parking policies fairly and efficiently.

## Introduction
In recent years, the demand for short-term parking has significantly increased in urban areas, especially near schools, public institutions, and city centers. Misuse of these zones such as drivers parking for long durations reduces efficiency and creates accessibility issues.  
At the same time, municipalities face challenges enforcing parking rules without invading citizens privacy. Existing systems rely on license plates, raising concerns about **data misuse, surveillance, and privacy violations**.  
Our solution tackles this problem by introducing a **privacy-preserving parking management system** based on AI-powered cameras. Instead of storing or processing license plates, the system anonymizes sensitive information and identifies vehicles solely through **non-identifiable attributes**. This approach balances effective enforcement with strict privacy requirements.

## Problem Statement
Two major issues motivated the project:  
1. **Parking Misuse** – Short-term zones are exploited for long-term parking, limiting availability for those who truly need them.  
2. **Privacy Concerns** – Current parking enforcement technologies rely on personally identifiable information, leading to risks of **data leakage, misuse, and lack of public trust**.  

Therefore, the challenge was to design a system that can:  
- Enforce parking duration limits.  
- Ensure real-time monitoring and reporting.  
- Maintain strict privacy protection and anonymization at every stage.  


## System Architecture
The system is designed as a **modular microservices architecture**, allowing scalability, maintainability, and independent development of components.  

**Core Components:**  
- **Vehicle Recognition Service (Python):** Processes images and extracts vehicle attributes (type, brand, color, damages).  
- **Comparison Service (Python):** Compares vehicles across multiple sessions to track overstays.  
- **Alert Service (Kotlin):** Issues alerts to municipalities when violations occur.  
- **Data Service (Kotlin + MongoDB):** Manages storage of vehicle metadata, time logs, and system events.  
- **Frontend (React.js):** Provides municipalities with an interface to view statistics, reports, and alerts.  
- **Orchestration (Docker Compose):** Ensures seamless deployment and scaling of services.  

### Key Controllers

<table>
  <thead>
    <tr>
      <th align="left">Controller</th>
      <th align="left">Description</th>
      <th align="left">Source</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><code>VehicleRecognitionController</code></td>
      <td>Vehicle detection pipeline (extracts type, make, color, damage, location; anonymizes faces & plates) and matches vehicles across sessions to determine overstays and update timestamps.</td>
      <td>
        <a href="https://github.com/Avichai98/AIBCS-M3p/blob/main/backend/services/python-services/controllers/vehicle_processing_controller.py">vehicle_processing_controller.py</a>
      </td>
    </tr>
    <tr>
      <td><code>AlertController</code></td>
      <td>Generates and dispatches violation alerts to the municipality.</td>
      <td>
        <a href="https://github.com/Avichai98/AIBCS-M3p/blob/main/backend/services/kotlin-services/AlertService/src/main/kotlin/app/alertservice/controllers/AlertController.kt">AlertController.kt</a>
      </td>
    </tr>
    <tr>
      <td><code>CameraController</code></td>
      <td>Handles interaction with physical cameras: capture, status, and ingestion.</td>
      <td>
        <a href="https://github.com/Avichai98/AIBCS-M3p/blob/main/backend/services/kotlin-services/DataService/src/main/kotlin/app/dataservice/controllers/CameraController.kt">CameraController.kt</a>
      </td>
    </tr>
    <tr>
      <td><code>UserController</code></td>
      <td>Manages users, roles, and access within the municipal portal.</td>
      <td>
        <a href="https://github.com/Avichai98/AIBCS-M3p/blob/main/backend/services/kotlin-services/DataService/src/main/kotlin/app/dataservice/controllers/UserController.kt">UserController.kt</a>
      </td>
    </tr>
  </tbody>
</table>

### Diagrams
- **Class Diagrams:** Show the main entities such as Vehicle, Alert, Camera, and User.  
- **Sequence Diagrams:** Demonstrate the flow from image capture → vehicle recognition → comparison → alert generation.  
- **State & Component Diagrams:** Illustrate system states, fault tolerance, and modular component interactions.  
- **Data Flow & Deployment Diagrams:** Show how information moves between services and how the system is deployed across infrastructure.
  
## Implementation Details
Important implementation references:  
- [process_image](backend/services/python-services/services/vehicle_processing_service.py#L106) – Vehicle recognition function.  
- [compare_all_vehicles_from_db](backend/services/python-services/services/vehicle_processing_service.py#L414) – Vehicle tracking logic.  
- [createAlert](backend/services/kotlin-services/AlertService/src/main/kotlin/app/alertservice/services/AlertServiceImpl.kt#L31) – Alert generation logic.  
- [frontend](frontend) – React-based municipal portal.  
- [tests](backend/services/python-services/tests) – Unit and integration tests.  

## Testing & Evaluation
Testing included:  
- **Detection Accuracy:** Achieved ~62% baseline, with Recall of 91% and Precision of 71%.  
- **Performance Stress Tests:** Simulated 100 recognition requests in parallel, achieving >95% within response time limits.  
- **Privacy Tests:** Simulated 50 unauthorized access attempts, all successfully blocked.  
- **Usability Tests:** 90% of municipal staff completed assigned tasks without training.  

## Results & Success Metrics
- Real-time detection achieved at 30 FPS.  
- Alerts correctly generated for ~60% of overstaying cases in prototype stage.  
- Fault tolerance: >95% recovery rate from simulated failures.  
- Positive usability feedback from stakeholders.  

## Lessons Learned
- Building a **privacy-first AI** system required trade-offs between accuracy and anonymization.  
- Microservices allowed easier scalability and isolated development of new features.  
- Clear metrics (accuracy, response time, usability) helped guide iterative improvements.  
- Real-world constraints, such as outdoor camera durability and varying lighting conditions, introduced additional challenges.  

## Future Work
- Improve recognition accuracy with multimodal AI models.  
- Extend coverage to public transport lanes and emergency vehicle monitoring.  
- Explore distributed edge-computing deployments for scalability.  
- Integrate with cloud platforms for large-scale city-wide rollout.

## Requirements
- docker
- download the yolov4.weights for vehicle detection from https://github.com/AlexeyAB/darknet/releases/download/darknet_yolo_v3_optimal/yolov4.weights and place it in backend\services\python-services\vehicle-recognition-api-yolov4-python-master\yolov4
### packages
- fastapi==0.115.12
- MNN==3.1.3
- numpy==1.26.4
- opencv_python==4.11.0.86
- Pillow==11.2.1
- pydantic==2.11.5
- ultralytics==8.3.102
- uvicorn[standard]==0.34.3
- python-multipart==0.0.20
- httpx==0.27.0
- kafka-python==2.2.13
- tzlocal==5.0.1
- azure-storage-blob==12.25.1
- python-jose[cryptography]==3.3.0
## Installation & Setup
```bash
git clone https://github.com/Avichai98/AIBCS-M3p.git
cd AIBCS-M3p\backend
docker-compose up --build
```
## Authors

<table>
  <tr>
    <td>
      <a href="https://github.com/tomtorh96">
        <img src="https://github.com/tomtorh96.png" width="120" style="border-radius:50%;" />
      </a>
    </td>
    <td>
      <b>Tomer Harel</b>
    </td>
  </tr>
  <tr>
    <td>
      <a href="https://github.com/Avichai98">
        <img src="https://github.com/Avichai98.png" width="120" style="border-radius:50%;" />
      </a>
    </td>
    <td>
      <b>Avichai Shchori</b>
    </td>
  </tr>
  <tr>
    <td>
      <img src="https://cdn-icons-png.flaticon.com/512/3135/3135715.png" width="120" style="border-radius:50%;" />
    </td>
    <td>
      <i>Supervisor:</i> Dr. Gennadi Birfir
    </td>
  </tr>
</table>
