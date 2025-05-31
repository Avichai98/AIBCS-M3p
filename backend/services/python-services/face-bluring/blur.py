from ultralytics import YOLO
import cv2
import numpy as np
# path =r"bus.jpg"
# model = YOLO(r"model.pt")  # Load a pretrained YOLOv8 model
# output = cv2.imread(path)  # Load an image
# if output is None:
#     raise ValueError("Image not found or unable to load.")
# results = model.predict(source=path)  # Predict on an image
# for result in results:
#     for box in result.boxes:
#         xxyy = box.xyxy.numpy()  # Get the bounding box coordinates as x1, y1, x2, y2
#         confidence = box.conf.numpy()[0]  # Get the confidence score
#         y1, x1, y2, x2 = int(xxyy[0][1]), int(xxyy[0][0]), int(xxyy[0][3]), int(xxyy[0][2])  # Extract coordinates
#         blurred = cv2.medianBlur(output[y1:y2, x1:x2], 25)
#         output[y1:y2, x1:x2] = blurred  # Replace the original image with the blurred image
#         # Add confidence score text
#         cv2.putText(output, f"{confidence:.2f}", (x1, y1 - 10), cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 255, 0), 1)
        
# cv2.imwrite("output.jpg", output)




class FaceBlur:
    def __init__(self, model_path):
        self.model = YOLO(model_path)

    def blur_faces(self, image_path,name):
        output = cv2.imread(image_path)
        if output is None:
            raise ValueError("Image not found or unable to load.")
        results = self.model.predict(source=image_path)
        for result in results:
            for box in result.boxes:
                xxyy = box.xyxy.numpy()
                confidence = box.conf.numpy()[0]
                y1, x1, y2, x2 = int(xxyy[0][1]), int(xxyy[0][0]), int(xxyy[0][3]), int(xxyy[0][2])
                blurred = cv2.medianBlur(output[y1:y2, x1:x2], 25)
                output[y1:y2, x1:x2] = blurred
                cv2.putText(output, f"{confidence:.2f}", (x1, y1 - 10), cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 255, 0), 1)
        name = fr"image_output\{name}.jpg"
        print(name)
        cv2.imwrite(name, output)
        return output
