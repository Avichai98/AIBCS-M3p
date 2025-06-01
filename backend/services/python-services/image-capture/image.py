from flask import Flask, jsonify, request
from cv2 import VideoCapture, imwrite
from datetime import datetime
import os

app = Flask(__name__)

def capture_image():
    cam_port = 1
    cam = VideoCapture(cam_port)
    result, image = cam.read()
    if result:
        name = datetime.now().strftime("%Y-%m-%d_%H-%M-%S")
        file_path = f"{name}.png"
        imwrite(file_path, image)
        return file_path
    else:
        return None

def remove_image(name):
    file_path = f"{name}.png"
    try:
        os.remove(file_path)
        return True
    except FileNotFoundError:
        return False
    except Exception as e:
        return str(e)

@app.route('/capture', methods=['POST'])
def api_capture():
    file_path = capture_image()
    if file_path:
        return jsonify({"status": "success", "image": file_path}), 200
    else:
        return jsonify({"status": "error", "message": "No image captured"}), 500

@app.route('/remove/<image_name>', methods=['DELETE'])
def api_remove(image_name):
    result = remove_image(image_name)
    if result is True:
        return jsonify({"status": "success", "message": f"{image_name}.png deleted"}), 200
    elif result is False:
        return jsonify({"status": "error", "message": f"{image_name}.png not found"}), 404
    else:
        return jsonify({"status": "error", "message": result}), 500

if __name__ == '__main__':
    app.run(debug=True)
