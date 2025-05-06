import numpy as np
import classifier
import cv2
import pprint
yolocfg = r'vehicle-recognition-api-yolov4-python-master\yolov4\yolov4.cfg'
yoloweights = r'vehicle-recognition-api-yolov4-python-master\yolov4\yolov4.weights'
modelbrandweights = r'C:\Users\Tomer\Documents\visual studio code\AIBCS-M3p\backend\services\python-services\vehicle-recognition-api-yolov4-python-master\model-weights-spectrico-car-brand-recognition-mobilenet_v3-224x224-170620.mnn'
modelcolorweights = r'C:\Users\Tomer\Documents\visual studio code\AIBCS-M3p\backend\services\python-services\vehicle-recognition-api-yolov4-python-master\model-weights-spectrico-car-colors-recognition-mobilenet_v3-224x224-180420.mnn'
image = r'C:\Users\Tomer\Documents\visual studio code\AIBCS-M3p\backend\services\python-services\vehicle-recognition-api-yolov4-python-master\bus.jpg'
lebels_colors = r'C:\Users\Tomer\Documents\visual studio code\AIBCS-M3p\backend\services\python-services\vehicle-recognition-api-yolov4-python-master\labels-colors.txt'
lebels_makes = r'C:\Users\Tomer\Documents\visual studio code\AIBCS-M3p\backend\services\python-services\vehicle-recognition-api-yolov4-python-master\labels-makes.txt'
coco_names = r'C:\Users\Tomer\Documents\visual studio code\AIBCS-M3p\backend\services\python-services\vehicle-recognition-api-yolov4-python-master\yolov4\coco.names'
def objectDetect(image_path):
    objects = []
    LABELS = open(coco_names).read().strip().split("\n")
    img = cv2.imread(image_path)
    if img is None:
        raise ValueError("Bad image")

    classes, confidences, boxes = net.detect(img, confThreshold=0.1, nmsThreshold=0.4)
    for classId, confidence, box in zip(classes.flatten(), confidences.flatten(), boxes):
        if classId in [2, 5, 7]:
            if confidence > 0.3:
                left, top, width, height = box
                x1 = left
                x2 = left + width
                y1 = top
                y2 = top + height
                car_img = img[y1:y2, x1:x2]
                (make, make_confidence) = car_make_classifier.predict(car_img)
                (color, color_confidence) = car_color_classifier.predict(car_img)
                rect = {"left": str(x1), "top": str(y1), "width": str(x2-x1), "height": str(y2-y1)}
                objects.append({"object":LABELS[classId],"make": make, "color": color, "make_prob": str(make_confidence), "color_prob": str(color_confidence), "object_prob": str(confidence), "rect": rect})
    
    return {'vehicles': objects}
    
net = cv2.dnn_DetectionModel(yolocfg, yoloweights)
net.setInputSize(608, 608)
net.setInputScale(1.0 / 255)
net.setInputSwapRB(True)
car_make_classifier = classifier.Classifier(modelbrandweights, lebels_makes)
car_color_classifier = classifier.Classifier(modelcolorweights, lebels_colors)
output = objectDetect(image)
pprint.pprint(output)