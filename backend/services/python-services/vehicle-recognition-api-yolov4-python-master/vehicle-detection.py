from flask import Flask, request, jsonify
import os
import cv2
from datetime import datetime
import classifier  
import numpy as np

app = Flask(__name__)
UPLOAD_FOLDER = 'uploads'
os.makedirs(UPLOAD_FOLDER, exist_ok=True)

# ========== Original Code (Adapted for API) ==========

def get_items():
    base_path = os.path.dirname(os.path.abspath(__file__))
    return [
        os.path.join(base_path, 'yolov4', 'yolov4.cfg'),
        os.path.join(base_path, 'yolov4', 'yolov4.weights'),
        os.path.join(base_path, 'model-weights-spectrico-car-brand-recognition-mobilenet_v3-224x224-170620.mnn'),
        os.path.join(base_path, 'model-weights-spectrico-car-colors-recognition-mobilenet_v3-224x224-180420.mnn'),
        os.path.join(base_path, 'labels-colors.txt'),
        os.path.join(base_path, 'labels-makes.txt'),
        os.path.join(base_path, 'yolov4', 'coco.names'),
    ]

class model():
    def __init__(self, yolocfg, yoloweights, modelbrandweights, modelcolorweights, labels_colors, labels_makes, coco_names):
        self.net = cv2.dnn_DetectionModel(yolocfg, yoloweights)
        self.net.setInputSize(608, 608)
        self.net.setInputScale(1.0 / 255)
        self.net.setInputSwapRB(True)
        self.car_make_classifier = classifier.Classifier(modelbrandweights, labels_makes)
        self.car_color_classifier = classifier.Classifier(modelcolorweights, labels_colors)
        self.coco_names = coco_names

    def objectDetect(self, image_path):
        objects = []
        LABELS = open(self.coco_names).read().strip().split("\n")
        img = cv2.imread(image_path)
        if img is None:
            raise ValueError("Bad image")

        classes, confidences, boxes = self.net.detect(img, confThreshold=0.1, nmsThreshold=0.4)
        for classId, confidence, box in zip(classes.flatten(), confidences.flatten(), boxes):
            if classId in [2, 5, 7] and confidence > 0.3:
                left, top, width, height = box
                x1, x2 = left, left + width
                y1, y2 = top, top + height
                car_img = img[y1:y2, x1:x2]
                make, make_conf = self.car_make_classifier.predict(car_img)
                color, color_conf = self.car_color_classifier.predict(car_img)
                rect = {"left": str(x1), "top": str(y1), "width": str(x2 - x1), "height": str(y2 - y1)}
                objects.append({
                    "object": LABELS[classId],
                    "make": make,
                    "color": color,
                    "make_prob": str(make_conf),
                    "color_prob": str(color_conf),
                    "object_prob": str(confidence),
                    "rect": rect
                })
        return {"vehicles": objects}

# ========== Load the Model Once ==========

paths = get_items()
mymodel = model(*paths)

# ========== API Endpoint ==========

@app.route('/detect', methods=['POST'])
def detect_vehicle():
    if 'image' not in request.files:
        return jsonify({'error': 'No image part in request'}), 400

    file = request.files['image']
    if file.filename == '':
        return jsonify({'error': 'No selected file'}), 400

    filename = f"input_{datetime.now().strftime('%Y%m%d_%H%M%S')}.jpg"
    filepath = os.path.join(UPLOAD_FOLDER, filename)
    file.save(filepath)

    try:
        result = mymodel.objectDetect(filepath)
        return jsonify(result), 200
    except Exception as e:
        return jsonify({'error': str(e)}), 500
    finally:
        os.remove(filepath)  # Optional cleanup

# ========== Start Server ==========

if __name__ == '__main__':
    app.run(debug=True)
