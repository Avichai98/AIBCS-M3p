import pytest
import httpx
import time

API_BUILD_URL = "http://localhost:5000/build"
API_URL = "http://localhost:5000/demo_work"
API_DELETE_URL = "http://localhost:8080/vehicles/deleteAllVehicles"
API_GET_URL = "http://localhost:8080/vehicles/getVehicles"

@pytest.mark.asyncio
async def test_vehicle_db_count_flow():
    async with httpx.AsyncClient() as client:
        # Step 1: clean up the database
        response = await client.delete(API_DELETE_URL, timeout=60.0)
        assert response.status_code == 200

        # Step 2: build models
        response = await client.get(API_BUILD_URL, timeout=60.0)
        assert response.status_code == 200
        data = response.json()
        assert data["message"] == "Models built successfully"
        
        time.sleep(2)
        # Step 3: Send first image
        with open("services/python-services/tests/test1.jpg", "rb") as f:
            response = await client.post(API_URL, files={"file1": ("test1.jpg", f, "image/jpeg")}, timeout=60.0)
            assert response.status_code == 200

        time.sleep(3)

        # Check that there are 3 vehicles in the database
        response = await client.get(API_GET_URL)
        assert response.status_code == 200
        data = response.json()
        assert isinstance(data, list)
        assert len(data) == 3

        time.sleep(3)

        # Step 4: Send second image
        with open("services/python-services/tests/test2.jpg", "rb") as f:
            response = await client.post(API_URL, files={"file1": ("test2.jpg", f, "image/jpeg")}, timeout=60.0)
            assert response.status_code == 200

        time.sleep(3)

        # Check that there are now 5 vehicles in the database
        response = await client.get(API_GET_URL)
        assert response.status_code == 200
        data = response.json()
        assert isinstance(data, list)
        assert len(data) == 5