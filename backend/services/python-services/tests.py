import sys
import os
import cv2
from PIL import Image  # Optional: only if you want to load the image
import glob
import ast
import re

sys.path.append(
    os.path.join(
        os.path.dirname(__file__), "vehicle-recognition-api-yolov4-python-master"
    )
)
from vehicle_detection import VehicleRecognitionModel, get_items

sys.path.append(
    os.path.join(os.path.dirname(__file__), "Damaged-Car-parts-prediction-Model")
)
from car_parts import set_detection

sys.path.append(
    os.path.join(os.path.dirname(__file__), "Damaged-Car-parts-prediction-Model")
)
from car_parts import set_detection
import random
from typing import List


def test_vehicle_recognition(
    location, image_extensions={".jpg", ".jpeg", ".png", ".bmp"}
):
    items = get_items()
    model = VehicleRecognitionModel(*items)
    model_prediction = []
    color_prediction = []
    print("starting vehicle recognition test")
    for subdir, dirs, files in os.walk(location):
        for file in files:
            ext = os.path.splitext(file)[1].lower()
            if ext in image_extensions:
                image_path = os.path.join(subdir, file)
                image_name = os.path.basename(file)
                name = image_name.split("$$")
                real_manufacturer = name[0]
                real_color = name[3]
                try:
                    output = model.objectDetect(image_path).get("vehicles")[0]
                    print(output)
                    make = output.get("make", "")
                    make_prob = output.get("make_prob", 0.0)
                    color = output.get("color", "")
                    color_prob = output.get("color_prob", 0.0)
                    man_answer = ""
                    color_answer = ""
                    if make.lower() == real_manufacturer.lower():
                        man_answer = "Manufacturer is correct"
                    else:
                        man_answer = "Manufacturer is incorrect"
                    if color.lower() == real_color.lower():
                        color_answer = "Color is correct"
                    else:
                        color_answer = "Color is incorrect"
                    model_prediction.append(
                        f"on {image_name} {man_answer}: {make} with {make_prob}"
                    )
                    color_prediction.append(
                        f"on {image_name} {color_answer}: {color} with {color_prob}"
                    )
                except Exception as e:
                    print(f"Error processing {image_path}: {e}")
                    model_prediction.append(f"on {image_name} Error: {str(e)}")
                    color_prediction.append(f"on {image_name} Error: {str(e)}")
    return model_prediction, color_prediction


sys.path.append(
    os.path.join(os.path.dirname(__file__), "Damaged-Car-parts-prediction-Model")
)
from car_parts import set_detection


def test_damage_detection(
    folder1: str, folder2: str, count: int = 100
) -> (List[str], List[str]):
    def get_image_files(folder):
        exts = (".jpg", ".jpeg", ".png", ".bmp", ".gif", ".tiff", ".webp")
        return [
            os.path.join(folder, f)
            for f in os.listdir(folder)
            if f.lower().endswith(exts)
        ]

    model = set_detection()
    images1 = get_image_files(folder1)
    images2 = get_image_files(folder2)
    damageList = []
    wholetList = []
    picked1 = random.sample(images1, min(count, len(images1)))
    picked2 = random.sample(images2, min(count, len(images2)))
    print("starting to predict")
    for img in picked1:
        image = cv2.imread(img)
        output = model(image)
        if len(output.get("classes", [])) > 0:
            damageList.append(
                f"correct on {img} found {output.get('classes', [])} with confidence {output.get('confidences', [])}"
            )
        else:
            damageList.append(f"incorrect on {img} there was damage")
    for img in picked2:
        image = cv2.imread(img)
        output = model(image)
        if len(output.get("classes", [])) > 0:
            wholetList.append(
                f"incorrect on {img} found {output.get('classes', [])} with confidence {output.get('confidences', [])}"
            )
        else:
            wholetList.append(f"correct on {img} there was no damage")
    return damageList, wholetList


def test_face_detection(file_path):
    total = 0
    found = 0
    with open(file_path, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip().rstrip(",")
            if not line:
                continue
            try:
                entry = ast.literal_eval(line)
                if isinstance(entry, dict):
                    value = list(entry.values())[0]
                    match = re.search(r"got only (\d+)/(\d+)", value)
                    if match:
                        x, y = map(int, match.groups())
                        # detections.append((x, y))
                        total += y
                        found += x
            except Exception as e:
                print(f"Skipping line due to error: {e}")
                continue

    return total, found



import re
import matplotlib.pyplot as plt


def analyze_model_predictions(file_path):
    # Read file
    with open(file_path, "r", encoding="utf-8") as f:
        lines = f.readlines()

    # Initialize counters and accumulators
    correct = 0
    incorrect = 0
    probabilities = []
    correct_probs = []
    incorrect_probs = []

    # Regex to match lines like "Manufacturer is correct: [...] with 0.98"
    pattern = re.compile(r"(correct|incorrect).*?with ([0-9.]+)")

    # Parse each line
    for line in lines:
        match = pattern.search(line)
        if match:
            status, prob = match.groups()
            prob = float(prob)
            probabilities.append(prob)
            if status == "correct":
                correct += 1
                correct_probs.append(prob)
            else:
                incorrect += 1
                incorrect_probs.append(prob)

    # Compute averages
    avg_prob = sum(probabilities) / len(probabilities) if probabilities else 0
    avg_correct_prob = sum(correct_probs) / len(correct_probs) if correct_probs else 0
    avg_incorrect_prob = (
        sum(incorrect_probs) / len(incorrect_probs) if incorrect_probs else 0
    )

    # Print results
    print("Correct Predictions:", correct)
    print("Incorrect Predictions:", incorrect)
    print("Average Probability:", avg_prob)
    print("Average Correct Probability:", avg_correct_prob)
    print("Average Incorrect Probability:", avg_incorrect_prob)

    # Plotting
    fig, axs = plt.subplots(2, 1, figsize=(8, 10))

    # Bar chart for prediction counts
    axs[0].bar(["Correct", "Incorrect"], [correct, incorrect], color=["green", "red"])
    axs[0].set_title("Correct vs Incorrect Predictions")
    axs[0].set_ylabel("Count")

    # Bar chart for average probabilities
    axs[1].bar(
        ["Average", "Correct Avg", "Incorrect Avg"],
        [avg_prob, avg_correct_prob, avg_incorrect_prob],
        color=["blue", "green", "red"],
    )
    axs[1].set_title("Average Probabilities")
    axs[1].set_ylabel("Probability")

    plt.tight_layout()
    plt.show()


model_answers, color_answers = test_vehicle_recognition(
    r"C:\Users\Tomer\Documents\ML\Confirmed_fronts\confirmed_fronts"
)
damage, whole = test_damage_detection(
    r"C:\Users\Tomer\Documents\ML\archive\data1a\training\00-damage",
    r"C:\Users\Tomer\Documents\ML\archive\data1a\training\01-whole",
)
file_path = "face_answers.txt"
y, x = test_face_detection(file_path)

with open("model_answers.txt", "w", encoding="utf-8") as f:
    for answer in model_answers:
        f.write(answer + "\n")

with open("color_answers.txt", "w", encoding="utf-8") as f:
    for answer in color_answers:
        f.write(answer + "\n")
with open("damage_answers.txt", "w", encoding="utf-8") as f:
    for answer in damage:
        f.write(answer + "\n")

with open("whole_answers.txt", "w", encoding="utf-8") as f:
    for answer in whole:
        f.write(answer + "\n")

analyze_model_predictions("color_answers.txt")
