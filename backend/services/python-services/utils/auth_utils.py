import httpx

AUTH_URL = "http://localhost:8080/users/login"
EMAIL = "admin@gmail.com"
PASSWORD = "admin"

async def get_auth_token():
    response = httpx.post(AUTH_URL, json={"email": EMAIL, "password": PASSWORD})
    response.raise_for_status()
    return response.json()["token"]
