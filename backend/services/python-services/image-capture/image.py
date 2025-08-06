from fastapi import HTTPException
from cv2 import VideoCapture
from datetime import datetime
import os
import pytz


class camera_use:
    def __init__(self, port=0):
        self.port = port
        self.location = "image_output"

    def capture_image(self):
        cam = VideoCapture(self.port)
        if not cam.isOpened():
            raise HTTPException(
                status_code=500,
                detail="Camera could not be opened. Please check the device or port.",
            )
        result, image = cam.read()
        cam.release()
        if image is None:
            raise HTTPException(
                status_code=500, detail="Problem taking image"
            )
        if result:
            name = (
                datetime.now()
                .astimezone(pytz.timezone("Asia/Jerusalem"))
                .strftime("%Y-%m-%d_%H-%M-%S")
            )
            base_path = os.path.dirname(os.path.abspath(__file__))
            location_name = os.path.join(base_path, "image_output", f"{name}.png")
            return {
                "image": image,
                "name": location_name,
                "message": "Image captured successfully.",
            }
        else:
            raise HTTPException(
                status_code=500, detail="No image detected. Please try again."
            )
