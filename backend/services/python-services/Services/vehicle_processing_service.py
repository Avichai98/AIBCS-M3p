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
import json
import pytz
from azure.storage.blob import BlobServiceClient
from utils.auth_utils import get_auth_token

sys.path.append(
    os.path.join(
        os.path.dirname(__file__), "..", "vehicle-recognition-api-yolov4-python-master"
    )
)
from vehicle_detection import VehicleRecognitionModel, get_items

sys.path.append(os.path.join(os.path.dirname(__file__), "..", "face-bluring"))
from blur import ImageBlur, load_model

sys.path.append(os.path.join(os.path.dirname(__file__), "..", "image-capture"))
from image import camera_use

sys.path.append(
    os.path.join(os.path.dirname(__file__), "..", "Damaged-Car-parts-prediction-Model")
)
from car_parts import set_detection
from utils.kafka_queue import create_vehicle, update_vehicle


def build():
    try:
        status = {
            "vehicle": "not initialized",
            "image_blur": "not initialized",
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
            models["image_blur"] = ImageBlur(model_path)
            status["image_blur"] = "initialized"
        except Exception as e:
            status["image_blur"] = f"error: {str(e)}"
        try:
            models["car_damage"] = set_detection()
            status["car_damage"] = "initialized"
        except Exception as e:
            status["car_damage"] = f"error: {str(e)}"
        try:
            # need to be port 1 when not running on a raspberry pi
            models["camera"] = camera_use(0)
            status["capture_image"] = "ready"
        except Exception as e:
            status["capture_image"] = f"error: {str(e)}"
        return {
            "message": "Model initialization status.",
            "status": status,
            "models": {
                "vehicle": models.get("vehicle"),
                "image_blur": models.get("image_blur"),
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

  
def process_image(image, models, camera_id):
    try:
        vehicle_model = models.get("vehicle")
        car_damage_model = models.get("car_damage")
        if not vehicle_model or not car_damage_model:
            raise HTTPException(status_code=500, detail="Models are not initialized.")
        vehicle_results = vehicle_model.objectDetect(image).get("vehicles")
        full_list = []
        for vehicle in vehicle_results:
            rect = vehicle.get("rect")
            car_img = image[
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
                "cameraId": camera_id,
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
            full_list.append(v)
        return {
            "vehicles": full_list,
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error processing image: {str(e)}")


def crop_image(image, model):
    base_path = os.path.dirname(os.path.abspath(__file__))
    folderPath = os.path.join(base_path, "..", "image_output")
    now = datetime.now().astimezone(pytz.timezone("Asia/Jerusalem"))
    name = now.strftime("%Y-%m-%d_%H-%M-%S") + f"-{now.microsecond // 1000:03d}"
    output_path = os.path.join(folderPath, f"{name}.png")
    blur_image = model.image_blur(image)
    cv2.imwrite(output_path, blur_image)
    return output_path

  
def demo_work(image_upload, models, camera_id, flag=0):
    """
    This function is a demo workflow that simulates the process of capturing an image,
    detecting vehicles, blurring faces, and detecting car damages.
    It uses the provided models for each step.
    """
    camera = models.get("camera")
    if flag == 1:
        camera_name = camera.capture_image()
        if not camera_name:
            raise HTTPException(status_code=500, detail="Camera is not working.")
        image = camera_name["image"]
        # If image is PIL, convert to numpy array
        if isinstance(image, Image.Image):
            image = cv2.cvtColor(np.array(image), cv2.COLOR_RGB2BGR)
    else:
        image = Image.open(io.BytesIO(image_upload)).convert("RGB")
        new_width, new_height = 1280, 720
        image = image.resize((new_width, new_height))
        image = cv2.cvtColor(np.array(image), cv2.COLOR_RGB2BGR)
    full_list = process_image(image, models, camera_id).get("vehicles", [])
    output = compare_all_vehicles_from_db(full_list, models, image, camera_id)

    return output


def work(models):
    # get all the models and check if they are not null
    global start_flag
    camera = models.get("camera")
    if not camera:
        raise HTTPException(status_code=500, detail="camera is not initialized.")
    # Capture an image from the camera
    while start_flag == 1:
        camera_name = camera.capture_image()
        if not camera_name:
            raise HTTPException(status_code=500, detail="Camera is not working.")
        image = camera_name["image"]
        new_width, new_height = 1280, 720
        image = image.resize((new_width, new_height))
        full_list = process_image(image, models).get("vehicles", [])
        compare_all_vehicles_from_db(full_list)
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
                if score > 70:
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
    Compare two vehicles and return similarity score (0-100%).
    Includes type, manufacturer, color, bbox, and optional damage.

    :param db_vehicle: dict from database
    :param image_vehicle: dict from image detection
    :param weights: dict with feature weights
    :return: float similarity score
    """

    def match_score(
        val1, val2, prob1, prob2, min_score=0.0
    ):  # increase min_score to 0.1 for more strict matching
        if val1.lower() != val2.lower():
            return 0.0
        return max((prob1 + prob2) / 2.0, min_score)

    def get_bbox(vehicle):
        l, t = vehicle.get("left", 0), vehicle.get("top", 0)
        r, b = l + vehicle.get("width", 0), t + vehicle.get("height", 0)
        return {"left": l, "top": t, "right": r, "bottom": b}

    def bbox_iou(box1, box2):
        xA = max(box1.get("left", 0), box2.get("left", 0))
        yA = max(box1.get("top", 0), box2.get("top", 0))
        xB = min(box1.get("right", 0), box2.get("right", 0))
        yB = min(box1.get("bottom", 0), box2.get("bottom", 0))
        inter_w = max(0, xB - xA)
        inter_h = max(0, yB - yA)
        inter_area = inter_w * inter_h
        area1 = (box1.get("right", 0) - box1.get("left", 0)) * (
            box1.get("bottom", 0) - box1.get("top", 0)
        )
        area2 = (box2.get("right", 0) - box2.get("left", 0)) * (
            box2.get("bottom", 0) - box2.get("top", 0)
        )
        union_area = area1 + area2 - inter_area + 1e-6
        iou = inter_area / union_area
        return min(1.0, max(0.0, iou))

    def damage_match(details1, details2):
        details1_class = details1.get("classes", "")
        details2_class = details2.get("classes", "")
        max_details = max(len(details1_class), len(details2_class))
        total_classes = 0
        for key in details1_class:
            if key in details2_class:
                total_classes += 1
        if max_details > 0:
            return total_classes / max_details
        if max_details == 0 and total_classes == 0:
            return 1.0
        # return 0.0

    def compute_dynamic_weights(db_vehicle, image_vehicle, base_total_weight=0.8):
        """
        Dynamically compute weights for type, manufacturer, and color based on average confidences.

        :param base_total_weight: how much total weight these 3 fields should have (e.g., 0.8)
        :return: dict with weights for 'type', 'manufacturer', 'color'
        """

        def avg_confidence(prob1, prob2):
            return (prob1 + prob2) / 2

        # Get confidences
        type_conf = avg_confidence(
            db_vehicle.get("typeProb", 0.0), image_vehicle.get("typeProb", 0.0)
        )
        manu_conf = avg_confidence(
            db_vehicle.get("manufacturerProb", 0.0),
            image_vehicle.get("manufacturerProb", 0.0),
        )
        color_conf = avg_confidence(
            db_vehicle.get("colorProb", 0.0), image_vehicle.get("colorProb", 0.0)
        )
        total_conf = type_conf + manu_conf + color_conf
        bbox_weight = 1.0 - (total_conf / 3.0)
        bbox_weight_raw = min(max(bbox_weight, 0.0), 1.0)

        if total_conf == 0:
            # fallback to equal weights
            return {
                "type": base_total_weight / 4,
                "manufacturer": base_total_weight / 4,
                "color": base_total_weight / 4,
                "bbox": base_total_weight / 4,
            }
        raw_weights = {
            "type": type_conf,
            "manufacturer": manu_conf,
            "color": color_conf,
            "bbox": bbox_weight_raw,
        }

        # Normalize all weights so they sum to 1.0
        total_raw = sum(raw_weights.values())
        weights = {k: v / total_raw for k, v in raw_weights.items()}
        print(f"Raw weights: {raw_weights}, Normalized weights: {weights}")
        # Normalize based on their proportional confidence
        return weights

    def compute_damage_weight(db_vehicle, image_vehicle, base_weight=0.05):
        # If both empty → it's still useful → return base weight
        db_has_damage = bool(db_vehicle.get("details", {}).get("classes"))
        img_has_damage = bool(image_vehicle.get("details", {}).get("classes"))

        if not db_has_damage and not img_has_damage:
            return base_weight

        # If only one has damage → suspicious → reduce trust
        if db_has_damage != img_has_damage:
            return base_weight * 0.2  # maybe very small weight

        # If both have damage → use average confidence
        def avg_conf(details):
            confs = details.get("confidences", [])
            return sum(confs) / len(confs) if confs else 0.0

        avg_conf = (
            avg_conf(db_vehicle["details"]) + avg_conf(image_vehicle["details"])
        ) / 2
        return base_weight * min(avg_conf, 1.0)

    weights = compute_dynamic_weights(db_vehicle, image_vehicle, base_total_weight=0.95)
    weights["damage"] = compute_damage_weight(db_vehicle, image_vehicle)

    # Compute scores
    type_score = match_score(
        db_vehicle["type"],
        image_vehicle["type"],
        db_vehicle.get("typeProb", 0.0),
        image_vehicle.get("typeProb", 0.0),
    )
    manufacturer_score = match_score(
        db_vehicle["manufacturer"],
        image_vehicle["manufacturer"],
        db_vehicle.get("manufacturerProb", 0.0),
        image_vehicle.get("manufacturerProb", 0.0),
    )
    color_score = match_score(
        db_vehicle["color"],
        image_vehicle["color"],
        db_vehicle.get("colorProb", 0.0),
        image_vehicle.get("colorProb", 0.0),
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


def compare_all_vehicles_from_db(detected_vehicles, models, image, camera_id="6884dd8be79f33241d1688ab"):
    """
    Connect to MongoDB, fetch all stored vehicles, and compare with the detected ones.

    :param models:
    :param image:
    :param camera_id:
    :param db_uri: MongoDB connection string
    :param db_name: Name of the database
    :param collection_name: Collection containing vehicle entries
    :param detected_vehicles: List of vehicle dicts from image
    :return: List of match results (dict with db_vehicle, detected_vehicle, score)
    """

    url = f"http://data-management-service:8080/vehicles/getVehiclesByCameraId/{camera_id}"

    try:
        Image_blur_model = models.get("image_blur")
    except Exception as e:
        raise HTTPException(
            status_code=500, detail=f"Image blur model not initialized: {str(e)}"
        )

    try:
        token = get_auth_token()
        headers = {"Authorization": f"Bearer {token}"}
        response = httpx.get(url, headers=headers)

        if response.status_code == 404:
            print("No vehicles found in the database.")
            vehicles = []

        elif response.status_code != 200:
            print(f"Failed to fetch vehicles: {response.text}")
            raise HTTPException(
                status_code=500, detail="Failed to fetch vehicles from database."
            )
        else:
            vehicles = response.json()
    except Exception as e:
        print(f"Error fetching vehicles: {e}")
        return None
    output = []
    if vehicles is not None and len(vehicles) > 0:
        for detected in detected_vehicles:
            match_found = False
            for stored in vehicles:
                score = compare_vehicles(
                    stored, detected
                )  # uses the function defined earlier
                print(f"Comparing {stored} with {detected}, score: {score}")
                if score > 70:
                    # Update the stored vehicle with the detected one
                    output.append(
                        {
                            "db_vehicle": stored,
                            "detected_vehicle": detected,
                            "score": score,
                        }
                    )
                    update_vehicle(stored)
                    match_found = True
                    vehicles.remove(stored)  # Remove matched vehicle from list
                    break

            if not match_found:
                car_img = image[
                    detected["top"] : detected["top"] + detected["height"],
                    detected["left"] : detected["left"] + detected["width"],
                ]
                output_path = crop_image(car_img, Image_blur_model)
                filename = os.path.basename(output_path)
                image_url = upload_to_azure(output_path, filename)
                detected["imageUrl"] = output_path
                remove_an_image(output_path)
                create_vehicle(detected)
    else:
        output = {"DB empty": detected_vehicles}
        for detected in detected_vehicles:
            car_img = image[
                detected["top"] : detected["top"] + detected["height"],
                detected["left"] : detected["left"] + detected["width"],
            ]
            output_path = crop_image(car_img, Image_blur_model)
            filename = os.path.basename(output_path)
            image_url = upload_to_azure(output_path, filename)
            detected["imageUrl"] = image_url
            remove_an_image(output_path)
            create_vehicle(detected)
    return output


def remove_images():
    base_path = os.path.dirname(os.path.abspath(__file__))
    folderPath = os.path.join(base_path, "image_output")
    if os.path.exists(folderPath):
        for filename in os.listdir(folderPath):
            file_path = os.path.join(folderPath, filename)
            if os.path.isfile(file_path):
                os.remove(file_path)
        return {"status": "All images deleted from image_output"}
    else:
        return {"status": "image_output folder does not exist"}


def remove_an_image(image_path):
    if os.path.exists(image_path):
        os.remove(image_path)
        return {"status": f"{image_path} deleted from image_output"}
    else:
        return {"status": f"{image_path} does not exist in image_output"}


def get_blob_service():
    connect_str = os.getenv("AZURE_STORAGE_CONNECTION_STRING")
    return BlobServiceClient.from_connection_string(connect_str)


def upload_to_azure(
    image_path: str, blob_name: str, container_name: str = "images"
) -> str:
    blob_service = get_blob_service()
    container_client = blob_service.get_container_client(container_name)

    try:
        container_client.create_container()
    except Exception:
        pass  # Container already exists

    with open(image_path, "rb") as data:
        container_client.upload_blob(name=blob_name, data=data, overwrite=True)

    # return the URL of the uploaded blob
    return f"https://{blob_service.account_name}.blob.core.windows.net/{container_name}/{blob_name}"
