from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from cv2 import VideoCapture, imwrite
from datetime import datetime
import os

app = FastAPI()


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
        if result:
            name = datetime.now().strftime("%Y-%m-%d_%H-%M-%S")
            # location_name = f"{self.location}/{name}.png"
            base_path = os.path.dirname(os.path.abspath(__file__))
            location_name = os.path.join(base_path, "image_output", f"{name}.png")
            imwrite(location_name, image)
            return {
                "image": image,
                "name": location_name,
                "message": "Image captured successfully.",
            }
        else:
            raise HTTPException(
                status_code=500, detail="No image detected. Please try again."
            )


@app.delete("/image/{name}")
def remove_image(name: str):
    filename = f"{name}.png"
    if not os.path.exists(filename):
        raise HTTPException(status_code=404, detail=f"Image {filename} not found.")
    try:
        os.remove(filename)
        return {"message": f"Image {filename} removed successfully."}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
