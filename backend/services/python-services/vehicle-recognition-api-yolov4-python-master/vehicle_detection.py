import cv2
import os
from tempfile import NamedTemporaryFile
import classifier
from cv2 import dnn_DetectionModel


def get_items():
    base_path = os.path.dirname(os.path.abspath(__file__))
    return [
        os.path.join(base_path, "yolov4", "yolov4.cfg"),
        os.path.join(base_path, "yolov4", "yolov4.weights"),
        os.path.join(
            base_path,
            "model-weights-spectrico-car-brand-recognition-mobilenet_v3-224x224-170620.mnn",
        ),
        os.path.join(
            base_path,
            "model-weights-spectrico-car-colors-recognition-mobilenet_v3-224x224-180420.mnn",
        ),
        os.path.join(base_path, "labels-colors.txt"),
        os.path.join(base_path, "labels-makes.txt"),
        os.path.join(base_path, "yolov4", "coco.names"),
    ]


# Singleton instances for classifiers
_brand_classifier = None
_color_classifier = None


def get_brand_classifier(modelbrandweights, labels_makes):
    global _brand_classifier
    if _brand_classifier is None:
        _brand_classifier = classifier.Classifier(modelbrandweights, labels_makes)
    return _brand_classifier


def get_color_classifier(modelcolorweights, labels_colors):
    global _color_classifier
    if _color_classifier is None:
        _color_classifier = classifier.Classifier(modelcolorweights, labels_colors)
    return _color_classifier


class VehicleRecognitionModel:
    def __init__(
        self,
        yolocfg,
        yoloweights,
        modelbrandweights,
        modelcolorweights,
        labels_colors,
        labels_makes,
        coco_names,
    ):
        self.net = dnn_DetectionModel(yolocfg, yoloweights)
        self.net.setInputSize(608, 608)
        self.net.setInputScale(1.0 / 255)
        self.net.setInputSwapRB(True)
        self.car_make_classifier = get_brand_classifier(modelbrandweights, labels_makes)
        self.car_color_classifier = get_color_classifier(
            modelcolorweights, labels_colors
        )
        self.LABELS = open(coco_names).read().strip().split("\n")

    def objectDetect(self, image):
        objects = []
        if isinstance(image, str):
            img = cv2.imread(image)
            if img is None:
                raise ValueError("Invalid or unreadable image")
        else:
            img = image.copy()
        classes, confidences, boxes = self.net.detect(
            img, confThreshold=0.1, nmsThreshold=0.4
        )
        for classId, confidence, box in zip(
            classes.flatten(), confidences.flatten(), boxes
        ):
            if classId in [2, 5, 7] and confidence > 0.3:
                left, top, width, height = box
                x1, y1, x2, y2 = left, top, left + width, top + height
                car_img = img[y1:y2, x1:x2]
                make, make_conf = self.car_make_classifier.predict(car_img)
                color, color_conf = self.car_color_classifier.predict(car_img)
                rect = {
                    "left": str(x1),
                    "top": str(y1),
                    "width": str(x2 - x1),
                    "height": str(y2 - y1),
                }
                objects.append(
                    {
                        "object": self.LABELS[classId],
                        "make": make,
                        "color": color,
                        "make_prob": str(make_conf),
                        "color_prob": str(color_conf),
                        "object_prob": str(confidence),
                        "rect": rect,
                    }
                )
        return {"vehicles": objects}
