from kafka import KafkaProducer
import json

producer = KafkaProducer(
    bootstrap_servers='kafka:9092',
    value_serializer=lambda v: json.dumps(v).encode('utf-8')
)


def create_vehicle(vehicle):
    print("Sending new vehicle:", vehicle)

    producer.send("vehicle-create", value=vehicle,
                  headers=[("__TypeId__", b"app.dataservice.boundaries.VehicleBoundary")])
    producer.flush()

def update_vehicle(vehicle):
    print("Sending updated vehicle:", vehicle)

    producer.send("vehicle-update", value=vehicle,
                  headers=[("__TypeId__", b"app.dataservice.boundaries.VehicleBoundary")])
    producer.flush()
