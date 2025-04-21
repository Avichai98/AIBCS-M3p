# Copyright © 2019 by Spectrico
# Licensed under the MIT License
# Based on the tutorial by Adrian Rosebrock: https://www.pyimagesearch.com/2018/11/12/yolo-object-detection-with-opencv/
# Usage: $ python car_color_classifier_yolo3.py --image cars.jpg

# import the necessary packages
import numpy as np
import argparse
import time
import cv2
import os
import classifier


def car_detection(image_path, yolo_path=r"model+color-yolov4\yolov4", confidence_threshold=0.5, nms_threshold=0.3):

	# Set default values for arguments
	#image_path = "bus.jpeg"  # Default path to input image

	car_color_classifier = classifier.Classifier()

	# load the COCO class labels our YOLO model was trained on
	labelsPath = os.path.sep.join([yolo_path, "coco.names"])
	LABELS = open(labelsPath).read().strip().split("\n")

	# initialize a list of colors to represent each possible class label
	np.random.seed(42)
	COLORS = np.random.randint(0, 255, size=(len(LABELS), 3),
		dtype="uint8")

	# derive the paths to the YOLO weights and model configuration
	weightsPath = os.path.sep.join([yolo_path, "yolov4.weights"])
	configPath = os.path.sep.join([yolo_path, "yolov4.cfg"])

	# load our YOLO object detector trained on COCO dataset (80 classes)
	#print("[INFO] loading YOLO from disk...")
	net = cv2.dnn.readNetFromDarknet(configPath, weightsPath)

	# load our input image and grab its spatial dimensions
	image = cv2.imread(image_path)
	#image = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)

	(H, W) = image.shape[:2]

	# determine only the *output* layer names that we need from YOLO
	layer_names = net.getLayerNames()
	output_layers = [layer_names[i- 1] for i in net.getUnconnectedOutLayers()]

	# construct a blob from the input image and then perform a forward
	# pass of the YOLO object detector, giving us our bounding boxes and
	# associated probabilities
	blob = cv2.dnn.blobFromImage(image, 1 / 255.0, (608, 608),
		swapRB=True, crop=False)
	net.setInput(blob)
	#start = time.time()
	outputs = net.forward(output_layers)
	#end = time.time()

	# show timing information on YOLO
	#print("[INFO] YOLO took {:.6f} seconds".format(end - start))

	# initialize our lists of detected bounding boxes, confidences, and
	# class IDs, respectively
	boxes = []
	confidences = []
	classIDs = []

	# loop over each of the layer outputs
	for output in outputs:
		# loop over each of the detections
		for detection in output:
			# extract the class ID and confidence (i.e., probability) of
			# the current object detection
			scores = detection[5:]
			classID = np.argmax(scores)
			confidence = scores[classID]

			# filter out weak predictions by ensuring the detected
			# probability is greater than the minimum probability
			if confidence > confidence_threshold:
				# scale the bounding box coordinates back relative to the
				# size of the image, keeping in mind that YOLO actually
				# returns the center (x, y)-coordinates of the bounding
				# box followed by the boxes' width and height
				box = detection[0:4] * np.array([W, H, W, H])
				(centerX, centerY, width, height) = box.astype("int")

				# use the center (x, y)-coordinates to derive the top and
				# and left corner of the bounding box
				x = int(centerX - (width / 2))
				y = int(centerY - (height / 2))

				# update our list of bounding box coordinates, confidences,
				# and class IDs
				boxes.append([x, y, int(width), int(height)])
				confidences.append(float(confidence))
				classIDs.append(classID)

	# apply non-maxima suppression to suppress weak, overlapping bounding
	# boxes
	idxs = cv2.dnn.NMSBoxes(boxes, confidences, confidence_threshold,
		nms_threshold)

	# Initialize a list to store detection details
	detections = []

	# ensure at least one detection exists
	if len(idxs) > 0:
		# loop over the indexes we are keeping
		for i in idxs.flatten():
			# extract the bounding box coordinates
			(x, y) = (boxes[i][0], boxes[i][1])
			(w, h) = (boxes[i][2], boxes[i][3])

			# draw a bounding box rectangle and label on the image
			color = [int(c) for c in COLORS[classIDs[i]]]
			if classIDs[i] in [2, 5, 7]:
				#start = time.time()
				result = car_color_classifier.predict(image[max(y,0):y + h, max(x,0):x + w])
				#end = time.time()
				# show timing information on MobileNet classifier
				#print("[INFO] classifier took {:.6f} seconds".format(end - start))
				text = "{}: {:.4f}".format(result[0]['color'], float(result[0]['prob']))
				cv2.putText(image, text, (x + 2, y + 20), cv2.FONT_HERSHEY_SIMPLEX,
							0.6, color, 2)
				# Add detection details to the list
				detections.append({
					"class": LABELS[classIDs[i]],
					"confidence": confidences[i],
					"bounding_box": (x, y, w, h),
					"color": result[0]['color'],
					"color_probability": float(result[0]['prob'])
				})
			else:
				# Add detection details for non-car objects
				detections.append({
					"class": LABELS[classIDs[i]],
					"confidence": confidences[i],
					"bounding_box": (x, y, w, h),
					"color": None,
					"color_probability": None
				})
			cv2.rectangle(image, (x, y), (x + w, y + h), color, 2)
			text = "{}: {:.4f}".format(LABELS[classIDs[i]], confidences[i])
			cv2.putText(image, text, (x, y - 5), cv2.FONT_HERSHEY_SIMPLEX,
				0.5, color, 2)

	# Print the list of detections
	# print("[INFO] Detections:")
	# for detection in detections:
	# 	print(detection)
	
	# #show the output image
	# cv2.namedWindow('Image', cv2.WINDOW_NORMAL)
	# cv2.resizeWindow('Image', W, H)
	# cv2.imshow("Image", image)
	# cv2.imwrite("output.jpg", image)
	# cv2.waitKey(0)
	# cv2.destroyAllWindows()
	return detections
