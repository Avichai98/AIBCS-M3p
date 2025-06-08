import sys
from fastapi import FastAPI, HTTPException, UploadFile, File
import os
import cv2
import numpy as np
import threading
import uvicorn
import time
from datetime import datetime
from PIL import Image
import io

sys.path.append(
    os.path.join(
        os.path.dirname(__file__), "vehicle-recognition-api-yolov4-python-master"
    )
)
from vehicle_detection import VehicleRecognitionModel, get_items

sys.path.append(os.path.join(os.path.dirname(__file__), "face-bluring"))
from blur import FaceBlur, load_model

sys.path.append(os.path.join(os.path.dirname(__file__), "image-capture"))
from image import camera_use

sys.path.append(
    os.path.join(os.path.dirname(__file__), "Damaged-Car-parts-prediction-Model")
)
from car_parts import set_detection
import base64

start_flag = 0
models = {}
app = FastAPI(
    title="AI Vehicle & Face Processing API",
    docs_url="/docs",  # Swagger UI (default: /docs)
    redoc_url="/redoc",  # ReDoc UI (default: /redoc)
    openapi_url="/openapi.json",  # OpenAPI schema path (default: /openapi.json)
)


def encode_image_to_base64(image):
    _, buffer = cv2.imencode(".png", image)
    return base64.b64encode(buffer).decode("utf-8")


@app.get("/build")
def build():
    try:
        item_paths = get_items()
        models["vehicle"] = VehicleRecognitionModel(*item_paths)
        model_path = load_model()
        models["face_blur"] = FaceBlur(model_path)
        models["car_damage"] = set_detection()
        models["camera"] = camera_use(1)
        return {
            "message": "Models initialized successfully.",
            "models": {
                "vehicle": "initialized",
                "face_blur": "initialized",
                "capture_image": "ready",
                "car_damage": "initialized",
            },
        }
    except Exception as e:
        raise HTTPException(
            status_code=500, detail=f"Error initializing models: {str(e)}"
        )


@app.post("/start")
def start():
    global start_flag
    start_flag = 1
    thread = threading.Thread(target=work)
    thread.start()
    return {"message": "Started"}


@app.post("/stop")
def stop():
    global start_flag
    start_flag = 0
    return {"message": "Stopped"}


@app.post("/demo")
async def process_image(file: UploadFile = File(...)):
    try:
        contents = await file.read()
        image = Image.open(io.BytesIO(contents)).convert("RGB")
        name = datetime.now().strftime("%Y-%m-%d_%H-%M-%S")
        location_name = f"image_output/{name}.png"
        vehicle_model = models.get("vehicle")
        face_blur_model = models.get("face_blur")
        car_damage_model = models.get("car_damage")
        if not vehicle_model or not face_blur_model or not car_damage_model:
            raise HTTPException(status_code=500, detail="Models are not initialized.")
        # Save the PIL image at location_name using cv2
        os.makedirs("image_output", exist_ok=True)
        cv2.imwrite(location_name, cv2.cvtColor(np.array(image), cv2.COLOR_RGB2BGR))
        vehicle_results = vehicle_model.objectDetect(location_name).get("vehicles")
        img = cv2.imread(location_name)
        full_list = []
        for vehicle in vehicle_results:
            rect = vehicle.get("rect")
            car_img = img[
                int(rect["top"]) : int(rect["top"]) + int(rect["height"]),
                int(rect["left"]) : int(rect["left"]) + int(rect["width"]),
            ]
            car_damage_results = car_damage_model(car_img)
            if not car_damage_results:
                raise HTTPException(
                    status_code=500, detail="Car damage detection failed."
                )
            v = {
                "id": 0,
                "type": vehicle.get("object"),
                "manufacturer": vehicle.get("make"),
                "color": vehicle.get("color"),
                "type_prob": float(vehicle.get("object_prob", 0)),
                "manufacturer_prob": float(vehicle.get("make_prob", 0)),
                "color_prob": float(vehicle.get("color_prob", 0)),
                "image": encode_image_to_base64(car_img),
                "timestamp": 0,
                "top": int(rect["top"]),
                "left": int(rect["left"]),
                "width": int(rect["width"]),
                "height": int(rect["height"]),
                "latitude": round(float(rect["top"]) + (float(rect["height"]) / 2), 3),
                "longitude": round(float(rect["left"]) + (float(rect["width"]) / 2), 3),
                "details": str(car_damage_results),
            }
            # combined_result = (vehicle, car_damage_results)
            full_list.append(v)
        # Draw bounding boxes and probabilities on the image
        for vehicle in vehicle_results:
            rect = vehicle.get("rect")
            object = vehicle.get("object", 0)
            object_prob = vehicle.get("object_prob", 0)
            color = vehicle.get("color", 0)
            color_prob = vehicle.get("color_prob", 0)
            make = vehicle.get("make", 0)
            make_prob = vehicle.get("make_prob", 0)
            label = f"{object} {float(object_prob):.2f} {make} {float(make_prob):.2f} {color} {float(color_prob):.2f}"
            x, y, w, h = (
                int(rect["left"]),
                int(rect["top"]),
                int(rect["width"]),
                int(rect["height"]),
            )
            cv2.rectangle(img, (x, y), (x + w, y + h), (0, 255, 0), 2)
            cv2.putText(
                img, label, (x, y - 10), cv2.FONT_HERSHEY_SIMPLEX, 1.2, (0, 255, 0), 3
            )

        output_image_path = f"image_output/{name}_detected.png"
        cv2.imwrite(output_image_path, img)
        # Blur faces in the captured image
        blurred_image = face_blur_model.blur_faces(output_image_path, output_image_path)
        return {
            "vehicles": full_list,
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error processing image: {str(e)}")


def work():
    # get all the models and check if they are not null
    global start_flag
    vehicle_model = models.get("vehicle")
    face_blur_model = models.get("face_blur")
    car_damage_model = models.get("car_damage")
    camera = models.get("camera")
    if not vehicle_model or not face_blur_model or not car_damage_model or not camera:
        raise HTTPException(status_code=500, detail="Models are not initialized.")
    # Capture an image from the camera
    while start_flag == 1:
        camera_name = camera.capture_image()
        if not camera_name:
            raise HTTPException(status_code=500, detail="Camera is not working.")
        # detect vehicles in the captured image
        vehicle_results = vehicle_model.objectDetect(camera_name["name"]).get(
            "vehicles"
        )
        # if not vehicle_results:
        #   raise HTTPException(status_code=500, detail="Vehicle detection failed.")
        # Read the captured image
        img = cv2.imread(camera_name["name"])
        full_list = []
        for vehicle in vehicle_results:
            rect = vehicle.get("rect")
            car_img = img[
                int(rect["top"]) : int(rect["top"]) + int(rect["height"]),
                int(rect["left"]) : int(rect["left"]) + int(rect["width"]),
            ]
            # detects damages in the vehicle image
            car_damage_results = car_damage_model(car_img)
            if not car_damage_results:
                raise HTTPException(
                    status_code=500, detail="Car damage detection failed."
                )
            # Combine vehicle tuple and car_damage_results tuple
            combined_result = (vehicle, car_damage_results)
            full_list.append(combined_result)
        # Blur faces in the captured image
        blurred_image = face_blur_model.blur_faces(
            camera_name["name"], camera_name["name"]
        )
        time.sleep(1)


if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=8082)
