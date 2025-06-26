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
from api_comandes import (
    compare_vehicles,
    build,
    process_image,
    start,
    stop,
    demo_work,
)

import traceback
from kafka_queue import update_vehicle


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


@app.put("/update_vehicle")
async def update_vehicle_route(vehicle: dict):
    update_vehicle(vehicle)
    return {"status": "Vehicle update sent"}


@app.post("/demo")
async def process_image_demo(file: UploadFile = File(...)):
    try:
        file_content = await file.read()
        image = Image.open(io.BytesIO(file_content)).convert("RGB")
        new_width, new_height = 1280, 720
        image = image.resize((new_width, new_height))
        answer = process_image(image, models)
        return answer
    except Exception as e:
        tb = traceback.format_exc()
        raise HTTPException(status_code=500, detail=f"{str(e)}\nLocation:\n{tb}")


@app.post("/demo_work")
async def demo_work_flow(file1: UploadFile = File(None)):
    global models
    flag = 0
    try:
        file_content = await file1.read() if file1 is not None else None
        if file_content is None:
            flag = 1
        output = demo_work(file_content, models, flag=flag)
        return output

    except Exception as e:
        tb = traceback.format_exc()
        raise HTTPException(status_code=500, detail=f"{str(e)}\nLocation:\n{tb}")


def encode_image_to_base64(image):
    _, buffer = cv2.imencode(".png", image)
    return base64.b64encode(buffer).decode("utf-8")


@app.get("/")
async def root():
    return RedirectResponse(url="/docs")


@app.post("/compare_vehicles")
async def compare_vehicles_endpoint(
    file1: UploadFile = File(...), file2: UploadFile = File(...)
):
    try:
        image1 = await file1.read()
        image2 = await file2.read()
        db_vehicle = json.loads(image1)["vehicles"]
        image_vehicle = json.loads(image2)["vehicles"]
        if not db_vehicle or not image_vehicle:
            raise HTTPException(
                status_code=400, detail="No vehicles found in the provided images."
            )
        results = []
        for db_v in db_vehicle:
            for img_v in image_vehicle:
                score = compare_vehicles(db_v, img_v)
                if score > 50:
                    results.append(
                        {"db_vehicle": db_v, "image_vehicle": img_v, "score": score}
                    )
        return {"results": results}
    except Exception as e:
        tb = traceback.format_exc()
        raise HTTPException(status_code=500, detail=f"{str(e)}\nLocation:\n{tb}")


if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=5000, reload=True)
