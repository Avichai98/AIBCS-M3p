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
from api_comandes import compare_vehicles, build, process_image, start, stop

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


start_flag = 0
models = {}
app = FastAPI(
    title="AI Vehicle & Face Processing API",
    docs_url="/docs",  # Swagger UI (default: /docs)
    redoc_url="/redoc",  # ReDoc UI (default: /redoc)
    openapi_url="/openapi.json",  # OpenAPI schema path (default: /openapi.json)
)


@app.get("/build")
def build_models():
    global models
    answers = build()
    models = answers["models"]
    return {
        "message": "Models built successfully",
        "status": answers["status"],
    }


@app.get("/start")
async def start_work():
    start()


@app.get("/stop")
async def stop_work():
    stop()


@app.post("/demo")
async def process_image_demo(file: UploadFile = File(...)):
    file_content = await file.read()
    answer = process_image(file_content, models)
    return answer


def encode_image_to_base64(image):
    _, buffer = cv2.imencode(".png", image)
    return base64.b64encode(buffer).decode("utf-8")


@app.get("/")
async def root():
    return RedirectResponse(url="/docs")


# from pymongo import MongoClient #might replace it
@app.post("/compare_vehicles")
async def compare_vehicles_endpoint(
    file1: UploadFile = File(...), file2: UploadFile = File(...)
):
    if not models:
        raise HTTPException(status_code=500, detail="Models not built yet")

    try:
        image1 = await file1.read()
        image2 = await file2.read()

        result = compare_vehicles(image1, image2, models)
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=8082, reload=True)
