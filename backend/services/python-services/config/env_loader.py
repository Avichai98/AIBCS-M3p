from dotenv import load_dotenv
import os

load_dotenv()  # Load from .env at root (docker-compose level)

def get_jwt_secret():
    return os.getenv("JWT_SECRET")