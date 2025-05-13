import numpy as np
import classifier
import cv2
import pprint
import os
import glob

image = r'vehicle-recognition-api-yolov4-python-master\bus.jpg'
def get_items():
    files = []

    base_path = os.path.dirname(os.path.abspath(__file__))
    files.append(os.path.join(base_path,'yolov4', 'yolov4.cfg'))
    files.append(os.path.join(base_path, 'yolov4', 'yolov4.weights'))
    files.append(os.path.join(base_path, 'model-weights-spectrico-car-brand-recognition-mobilenet_v3-224x224-170620.mnn'))
    files.append(os.path.join(base_path, 'model-weights-spectrico-car-colors-recognition-mobilenet_v3-224x224-180420.mnn'))
    files.append(os.path.join(base_path, 'labels-colors.txt'))
    files.append(os.path.join(base_path, 'labels-makes.txt'))
    files.append(os.path.join(base_path, 'yolov4', 'coco.names'))
    return files

class model():
    def __init__(self, yolocfg, yoloweights,modelbrandweights, modelcolorweights, labels_colors, labels_makes,coco_names):
        self.net = cv2.dnn_DetectionModel(yolocfg, yoloweights)
        self.net.setInputSize(608, 608)
        self.net.setInputScale(1.0 / 255)
        self.net.setInputSwapRB(True)
        self.car_make_classifier = classifier.Classifier(modelbrandweights, labels_makes)
        self.car_color_classifier = classifier.Classifier(modelcolorweights, labels_colors)
        self.coco_names = coco_names

    def objectDetect(self,image_path):
        """
        def objectDetect(self, image_path):
            Detects vehicles in an image, identifies their make and color, and returns detailed information about each detected vehicle.
            Args:
                image_path (str): The file path to the image in which objects are to be detected.
            Returns:
                dict: A dictionary containing a list of detected vehicles. Each vehicle is represented as a dictionary with the following keys:
                    - "object" (str): The label of the detected object (e.g., "car").
                    - "make" (str): The predicted make of the vehicle.
                    - "color" (str): The predicted color of the vehicle.
                    - "make_prob" (str): The confidence score for the predicted make.
                    - "color_prob" (str): The confidence score for the predicted color.
                    - "object_prob" (str): The confidence score for the detected object.
                    - "rect" (dict): A dictionary containing the bounding box coordinates of the detected object:
                        - "left" (str): The x-coordinate of the top-left corner.
                        - "top" (str): The y-coordinate of the top-left corner.
                        - "width" (str): The width of the bounding box.
                        - "height" (str): The height of the bounding box.
            Raises:
                ValueError: If the provided image is invalid or cannot be read.
            Notes:
                - The function uses a pre-trained YOLOv4 model to detect objects in the image.
                - Only vehicles with class IDs 2, 5, and 7 (e.g., cars, buses, trucks) are considered.
                - The function also uses separate classifiers to predict the make and color of the detected vehicles.
        """
        objects = []
        LABELS = open(self.coco_names).read().strip().split("\n")
        img = cv2.imread(image_path)
        if img is None:
            raise ValueError("Bad image")

        classes, confidences, boxes = self.net.detect(img, confThreshold=0.1, nmsThreshold=0.4)
        for classId, confidence, box in zip(classes.flatten(), confidences.flatten(), boxes):
            if classId in [2, 5, 7]:
                if confidence > 0.3:
                    left, top, width, height = box
                    x1 = left
                    x2 = left + width
                    y1 = top
                    y2 = top + height
                    car_img = img[y1:y2, x1:x2]
                    (make, make_confidence) = self.car_make_classifier.predict(car_img)
                    (color, color_confidence) = self.car_color_classifier.predict(car_img)
                    rect = {"left": str(x1), "top": str(y1), "width": str(x2-x1), "height": str(y2-y1)}
                    objects.append({"object":LABELS[classId],"make": make, "color": color, "make_prob": str(make_confidence), "color_prob": str(color_confidence), "object_prob": str(confidence), "rect": rect})
        
        return {'vehicles': objects}

if __name__ == "__main__":    
    paths = get_items()
    mymodel = model(paths[0], paths[1], paths[2], paths[3], paths[4], paths[5], paths[6])
    output = mymodel.objectDetect(image)
    pprint.pprint(output)
