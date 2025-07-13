from fastapi import FastAPI, UploadFile, File, HTTPException
from fastapi.responses import FileResponse
from pydantic import BaseModel
from ultralytics import YOLO
import cv2
import os
import numpy as np
from datetime import datetime
import pytz

app = FastAPI()


def load_model():
    paths = []
    base_path = os.path.dirname(os.path.abspath(__file__))
    paths.append(os.path.join(base_path, "model.pt"))
    paths.append(os.path.join(base_path, "license-plate-finetune-v1n.pt"))
    return paths


class ImageBlur:
    def __init__(self, paths):
        self.face_model = YOLO(paths[0])  # Face detection model
        self.license_plate_model = YOLO(paths[1])  # License plate detection model

    def image_blur(self, image_path):
        def bluring(results, output):
            for result in results:
                for box in result.boxes:
                    xxyy = box.xyxy.numpy()
                    confidence = box.conf.numpy()[0]
                    y1, x1, y2, x2 = (
                        int(xxyy[0][1]),
                        int(xxyy[0][0]),
                        int(xxyy[0][3]),
                        int(xxyy[0][2]),
                    )
                    blurred = cv2.medianBlur(output[y1:y2, x1:x2], 25)
                    output[y1:y2, x1:x2] = blurred
            return output

        if isinstance(image_path, np.ndarray):
            output = image_path.copy()
            # base_path = os.path.dirname(os.path.abspath(__file__))
            # folderPath = os.path.join(base_path, "image_output")
            # name = (
            #     datetime.now()
            #     .astimezone(pytz.timezone("Asia/Jerusalem"))
            #     .strftime("%Y-%m-%d_%H-%M-%S"))
            # output_path = os.path.join(folderPath, f"{name}.png")

        else:
            output = cv2.imread(image_path)
            if output is None:
                raise ValueError("Image not found or unable to load.")
            output_path = image_path
        face_results = self.face_model.predict(source=image_path)
        plate_results = self.license_plate_model.predict(source=image_path)
        output = bluring(face_results, output)
        output = bluring(plate_results, output)
        # cv2.imwrite(output_path, output)
        return output
