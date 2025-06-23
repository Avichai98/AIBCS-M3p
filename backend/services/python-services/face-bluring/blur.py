from fastapi import FastAPI, UploadFile, File, HTTPException
from fastapi.responses import FileResponse
from pydantic import BaseModel
from ultralytics import YOLO
import cv2
import os
import numpy as np
from datetime import datetime

app = FastAPI()


# base_path = os.path.dirname(os.path.abspath(__file__))
# path = os.path.join(base_path,"model.pt")
# face_blur_model = YOLO(path)  # Update with your actual model path
def load_model():
    base_path = os.path.dirname(os.path.abspath(__file__))
    path = os.path.join(base_path, "model.pt")
    return path


class FaceBlur:
    def __init__(self, model):
        # self.model = model
        self.model = YOLO(model)

    def blur_faces(self, image_path, name):
        output = cv2.imread(image_path)
        if output is None:
            raise ValueError("Image not found or unable to load.")
        results = self.model.predict(source=image_path)
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
                # cv2.putText(
                #     output,
                #     f"{confidence:.2f}",
                #     (x1, y1 - 10),
                #     cv2.FONT_HERSHEY_SIMPLEX,
                #     0.5,
                #     (0, 255, 0),
                #     1,
                # )

        # output_dir = "image_output"
        # os.makedirs(output_dir, exist_ok=True)
        # output_path = os.path.join(output_dir, f"{name}.jpg")
        cv2.imwrite(name, output)
        return name


# face_blur = FaceBlur(face_blur_model)


# @app.post("/blur")
# async def blur_faces(file: UploadFile = File(...)):
#     try:
#         contents = await file.read()
#         name = datetime.now().strftime("%Y-%m-%d_%H-%M-%S")
#         temp_path = f"temp_{name}.jpg"
#         with open(temp_path, "wb") as f:
#             f.write(contents)
#         output_path = FaceBlur.blur_faces(temp_path, name)  # BAD
#         os.remove(temp_path)
#         return FileResponse(
#             output_path, media_type="image/jpeg", filename=os.path.basename(output_path)
#         )
#     except Exception as e:
#         raise HTTPException(status_code=500, detail=str(e))
