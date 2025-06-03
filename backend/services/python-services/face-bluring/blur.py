from flask import Flask, request, jsonify
import cv2
import os
from ultralytics import YOLO
from datetime import datetime

app = Flask(__name__)
OUTPUT_DIR = "image_output"
os.makedirs(OUTPUT_DIR, exist_ok=True)

class FaceBlur:
    def __init__(self, model_path):
        self.model = YOLO(model_path)

    def blur_faces(self, image_path, name):
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
                cv2.putText(output, f"{confidence:.2f}", (x1, y1 - 10),
                            cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 255, 0), 1)
        output_path = os.path.join(OUTPUT_DIR, f"{name}.jpg")
        cv2.imwrite(output_path, output)
        return output_path
    
base_path = os.path.dirname(os.path.abspath(__file__))
path = os.path.join(base_path,"model.pt")
# Load YOLO model once when the app starts
face_blur = FaceBlur(path)  # Replace with your actual model path

@app.route('/blur', methods=['POST'])
def blur_image():
    if 'image' not in request.files:
        return jsonify({'error': 'No image part in request'}), 400
    
    file = request.files['image']
    if file.filename == '':
        return jsonify({'error': 'No selected file'}), 400
    
    temp_path = os.path.join(OUTPUT_DIR, f"temp_{datetime.now().strftime('%Y%m%d_%H%M%S')}.jpg")
    file.save(temp_path)

    try:
        timestamp = datetime.now().strftime("%Y-%m-%d_%H-%M-%S")
        output_path = face_blur.blur_faces(temp_path, timestamp)
        os.remove(temp_path)
        return jsonify({'status': 'success', 'output_image': output_path}), 200
    except Exception as e:
        return jsonify({'error': str(e)}), 500

if __name__ == '__main__':
    app.run(debug=True)
