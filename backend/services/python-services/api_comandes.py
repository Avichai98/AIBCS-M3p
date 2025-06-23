import sys
from fastapi import FastAPI, HTTPException, UploadFile, File
from fastapi.responses import RedirectResponse
import os
import cv2
import numpy as np
import threading
import time
from datetime import datetime
from PIL import Image
import io
import httpx
import uvicorn
import base64
import json

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
from kafka_queue import create_vehicle


def build():
    try:
        status = {
            "vehicle": "not initialized",
            "face_blur": "not initialized",
            "capture_image": "not ready",
            "car_damage": "not initialized",
        }
        models = {}
        try:
            item_paths = get_items()
            models["vehicle"] = VehicleRecognitionModel(*item_paths)
            status["vehicle"] = "initialized"
        except Exception as e:
            status["vehicle"] = f"error: {str(e)}"
        try:
            model_path = load_model()
            models["face_blur"] = FaceBlur(model_path)
            status["face_blur"] = "initialized"
        except Exception as e:
            status["face_blur"] = f"error: {str(e)}"
        try:
            models["car_damage"] = set_detection()
            status["car_damage"] = "initialized"
        except Exception as e:
            status["car_damage"] = f"error: {str(e)}"
        try:
            models["camera"] = camera_use(1)
            status["capture_image"] = "ready"
        except Exception as e:
            status["capture_image"] = f"error: {str(e)}"
        return {
            "message": "Model initialization status.",
            "status": status,
            "models": {
                "vehicle": models.get("vehicle"),
                "face_blur": models.get("face_blur"),
                "car_damage": models.get("car_damage"),
                "camera": models.get("camera"),
            },
        }
    except Exception as e:
        raise HTTPException(
            status_code=500, detail=f"Error initializing models: {str(e)}"
        )


def start():
    global start_flag
    start_flag = 1
    thread = threading.Thread(target=work)
    thread.start()
    return {"message": "Started"}


def stop():
    global start_flag
    start_flag = 0
    return {"message": "Stopped"}


# async def process_image(file: UploadFile = File(...)):
def process_image(image, models):
    try:
        name = datetime.now().strftime("%Y-%m-%d_%H-%M-%S")
        base_path = os.path.dirname(os.path.abspath(__file__))
        location_name = os.path.join(base_path, "image_output", f"{name}.png")
        # location_name = f"{base_path}/image_output/{name}.png"
        vehicle_model = models.get("vehicle")
        face_blur_model = models.get("face_blur")
        car_damage_model = models.get("car_damage")
        if not vehicle_model or not face_blur_model or not car_damage_model:
            raise HTTPException(status_code=500, detail="Models are not initialized.")
        # Save the PIL image at location_name using cv2
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
                "cameraId": "6859134254232e6caafefef7",
                "type": str(vehicle.get("object")),
                "manufacturer": str(vehicle.get("make")),
                "color": str(vehicle.get("color")),
                "typeProb": float(vehicle.get("object_prob", 0)),
                "manufacturerProb": float(vehicle.get("make_prob", 0)),
                "colorProb": float(vehicle.get("color_prob", 0)),
                "imageUrl": "none",  # str(encode_image_to_base64(car_img)),
                "description": str(car_damage_results),
                "stayDuration": 0,
                "top": int(rect["top"]),
                "left": int(rect["left"]),
                "width": int(rect["width"]),
                "height": int(rect["height"]),
                "latitude": round(float(rect["top"]) + (float(rect["height"]) / 2), 3),
                "longitude": round(float(rect["left"]) + (float(rect["width"]) / 2), 3),
            }

            # Create a vehicle in a database
            #create_vehicle(v)  # can be problem be careful
            create_vehicle(v)

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

        # output_image_path = f"image_output/{name}_detected.png"
        output_image_path = os.path.join(
            base_path, "image_output", f"{name}_detected.png"
        )

        cv2.imwrite(output_image_path, img)
        # Blur faces in the captured image
        blurred_image = face_blur_model.blur_faces(output_image_path, output_image_path)
        return {
            "vehicles": full_list,
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error processing image: {str(e)}")


def demo_work_flow(models):
    """
    This function is a demo workflow that simulates the process of capturing an image,
    detecting vehicles, blurring faces, and detecting car damages.
    It uses the provided models for each step.
    """
    name = datetime.now().strftime("%Y-%m-%d_%H-%M-%S")
    base_path = os.path.dirname(os.path.abspath(__file__))
    location_name = os.path.join(base_path, "image_output", f"{name}.png")
    vehicle_model = models.get("vehicle")
    face_blur_model = models.get("face_blur")
    car_damage_model = models.get("car_damage")
    camera = models.get("camera")
    if not vehicle_model or not face_blur_model or not car_damage_model or not camera:
        raise HTTPException(status_code=500, detail="Models are not initialized.")
    # Capture an image from the camera
    camera_name = camera.capture_image()
    if not camera_name:
        raise HTTPException(status_code=500, detail="Camera is not working.")
    # detect vehicles in the captured image
    vehicle_results = vehicle_model.objectDetect(camera_name["name"]).get("vehicles")
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
            raise HTTPException(status_code=500, detail="Car damage detection failed.")
        # Combine vehicle tuple and car_damage_results tuple
        combined_result = (vehicle, car_damage_results)
        full_list.append(combined_result)
    # Blur faces in the captured image
    blurred_image = face_blur_model.blur_faces(camera_name["name"], camera_name["name"])


def work(models):
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


def compare_vehicles_from_files(db_vehicle_data, image_vehicle_data):
    """
    Accepts two JSON files:
    - db_vehicle_file: JSON file with db_vehicle object
    - image_vehicle_file: JSON file with image_vehicle object

    Returns the similarity score.
    """
    try:
        db_vehicle = json.loads(db_vehicle_data)["vehicles"]
        image_vehicle = json.loads(image_vehicle_data)["vehicles"]
        results = []
        for db_v in db_vehicle:
            for img_v in image_vehicle:
                score = compare_vehicles(db_v, img_v)
                if score > 50:
                    results.append(
                        {"db_vehicle": db_v, "image_vehicle": img_v, "score": score}
                    )
        # Optionally, you could return all results or process them further
        return {"results": results}
    except Exception as e:
        raise HTTPException(
            status_code=500, detail=f"Error comparing vehicles from files: {str(e)}"
        )


def compare_vehicles(db_vehicle, image_vehicle, weights=None):
    """
    Compare two vehicles and return similarity score (0–100%).
    Includes type, manufacturer, color, bbox, and optional damage.

    :param db_vehicle: dict from database
    :param image_vehicle: dict from image detection
    :param weights: dict with feature weights
    :return: float similarity score
    """
    if weights is None:
        weights = {
            "type": 0.3,
            "manufacturer": 0.15,
            "color": 0.3,
            "bbox": 0.2,
            "damage": 0.05,
        }

    def match_score(val1, val2, prob1, prob2, min_score=0.90):
        if val1.lower() != val2.lower():
            return 0.0
        return max((prob1 + prob2) / 2.0, min_score)

    def get_bbox(vehicle):
        l, t = vehicle["left"], vehicle["top"]
        r, b = l + vehicle["width"], t + vehicle["height"]
        return {"left": l, "top": t, "right": r, "bottom": b}

    def bbox_iou(box1, box2):
        xA = max(box1["left"], box2["left"])
        yA = max(box1["top"], box2["top"])
        xB = min(box1["right"], box2["right"])
        yB = min(box1["bottom"], box2["bottom"])
        inter_w = max(0, xB - xA)
        inter_h = max(0, yB - yA)
        inter_area = inter_w * inter_h
        area1 = (box1["right"] - box1["left"]) * (box1["bottom"] - box1["top"])
        area2 = (box2["right"] - box2["left"]) * (box2["bottom"] - box2["top"])
        union_area = area1 + area2 - inter_area + 1e-6
        iou = inter_area / union_area
        return min(1.0, max(0.0, iou))

    def damage_match(details1, details2):
        details1_class = details1.get("classes", "")
        details2_class = details2.get("classes", "")
        max_details = max(len(details1), len(details2))
        total_classes = 0
        for key in details1_class:
            if key in details2_class:
                total_classes += 1
        if max_details > 0:
            return total_classes / max_details
        if max_details == 0 and total_classes == 0:
            return 1.0
        # return 0.0

    # Compute scores
    type_score = match_score(
        db_vehicle["type"],
        image_vehicle["type"],
        db_vehicle.get("type_prob", 0.0),
        image_vehicle.get("type_prob", 0.0),
    )
    manufacturer_score = match_score(
        db_vehicle["manufacturer"],
        image_vehicle["manufacturer"],
        db_vehicle.get("manufacturer_prob", 0.0),
        image_vehicle.get("manufacturer_prob", 0.0),
    )
    color_score = match_score(
        db_vehicle["color"],
        image_vehicle["color"],
        db_vehicle.get("color_prob", 0.0),
        image_vehicle.get("color_prob", 0.0),
    )

    # Bounding box IoU with soft boost
    bbox1 = get_bbox(db_vehicle)
    bbox2 = get_bbox(image_vehicle)
    bbox_score = bbox_iou(bbox1, bbox2)
    if bbox_score > 0.5:
        bbox_score = max(bbox_score, 0.95)

    # Damage match (exact)
    damage_score = damage_match(
        db_vehicle.get("details", {}), image_vehicle.get("details", {})
    )

    # Final weighted total
    total_score = (
        weights["type"] * type_score
        + weights["manufacturer"] * manufacturer_score
        + weights["color"] * color_score
        + weights["bbox"] * bbox_score
        + weights["damage"] * damage_score
    )

    return round(total_score * 100, 2)


def create_vehicl(v):
    url = "http://data-management-service:8080/vehicles/create"
    print(v)
    with httpx.AsyncClient() as client:
        response = client.post(url, json=v)
        if response.status_code != 200:
            print(f"Failed to create vehicle: {response.text}")
            raise HTTPException(
                status_code=500, detail="Failed to create vehicle in database."
            )
    return response.json()


def compare_all_vehicles_from_db(db_uri, db_name, collection_name, detected_vehicles):
    """
    Connect to MongoDB, fetch all stored vehicles, and compare with the detected ones.

    :param db_uri: MongoDB connection string
    :param db_name: Name of the database
    :param collection_name: Collection containing vehicle entries
    :param detected_vehicles: List of vehicle dicts from image
    :return: List of match results (dict with db_vehicle, detected_vehicle, score)
    """
    url = "http://data-management-service:8080/vehicles/getVehicles"
    with httpx.AsyncClient() as client:
        response = client.get(url)
        if response.status_code != 200:
            print(f"Failed to fetch vehicles: {response.text}")
            return None
    vehicles = response.json()
    if not vehicles:
        raise HTTPException(
            status_code=404, detail="No vehicles found in the database."
        )
    for detected in detected_vehicles:
        for stored in vehicles:
            score = compare_vehicles(
                stored, detected
            )  # uses the function defined earlier
            if score > 0.7:
                # increase the total time by 1 second for each match
                detected["total_time"] += 1
