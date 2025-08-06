from ultralytics import YOLO
import cv2
import os
import numpy as np


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
        else:
            output = cv2.imread(image_path)
            if output is None:
                raise ValueError("Image not found or unable to load.")
        face_results = self.face_model.predict(source=image_path)
        plate_results = self.license_plate_model.predict(source=image_path)
        output = bluring(face_results, output)
        output = bluring(plate_results, output)
        return output
