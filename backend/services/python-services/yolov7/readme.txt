got from https://github.com/WongKinYiu/yolov7.git


run this in the main function to get the output from detect.py

import subprocess
import json

# Run the subprocess 
subprocess.run(["python", "detect.py", "--source", "inference/images/1.jpg","--weights", "yolov7.pt",
 "--source", "inference/images", "--img-size", "640", "--conf-thres", "0.30",
  "--iou-thres", "0.45",  "--project", "runs/detect","--name", "output_images"])

# Read the output
with open("output.json") as f:
    output_array = json.load(f)

print(output_array)
