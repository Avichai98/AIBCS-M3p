from cv2 import *
from datetime import datetime
import os
def capture_image():
    """
    This function captures an image from the default camera
    and saves it 
    """
    #the number is based on the number of webcams on the computer -1
    cam_port = 0
    cam = VideoCapture(cam_port)
    result, image = cam.read()
    if result:
        name = datetime.now().strftime("%Y-%m-%d_%H-%M-%S")
        # showing result, it take frame name and image output
        #imshow(name, image)
        
        # saving image in local storage
        imwrite(f"{name}.png", image)

        # If keyboard interrupt occurs, destroy image window
        # waitKey(0)
        # destroyWindow(name)
    else:
        #TODO: handle the case when no image is captured
        print("No image detected. Please! try again")
    return image,name

def remove_image(name):
    """
    This function removes the image file with the given name
    """
    try:
        os.remove(f"{name}.png")
        #print(f"Image {name}.png removed successfully.")
    #TODO: handle the case when image file does not exist or error occurs
    except FileNotFoundError:
        print(f"Image {name}.png not found.")
    except Exception as e:
        print(f"An error occurred: {e}")  
    

