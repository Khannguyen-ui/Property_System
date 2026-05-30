import os
import json
from kafka import KafkaConsumer

from app.feature import extract_features
from app.model import predict_score
from app.redis_client import save_score
from app.utils.logger import get_logger

logger = get_logger(__name__)

KAFKA_BOOTSTRAP_SERVERS = os.getenv(
    "KAFKA_BOOTSTRAP_SERVERS",
    "localhost:9092"
)


def start_consumer():

    logger.info(f"Connecting Kafka: {KAFKA_BOOTSTRAP_SERVERS}")

    consumer = KafkaConsumer(
        "user-behavior",
        bootstrap_servers=KAFKA_BOOTSTRAP_SERVERS,
        value_deserializer=lambda x: json.loads(
            x.decode("utf-8")
        ),
        auto_offset_reset="earliest",
        enable_auto_commit=True,
        group_id="recommendation-ml-group"
    )

    logger.info("Kafka Consumer Started")

    for message in consumer:

        try:
            event = message.value

            logger.info(f"Received Event: {event}")

            user_id = event["userId"]
            item_id = event["itemId"]
            item_type = event.get("itemType", "property")

            features = extract_features(event)

            score = predict_score(features)

            save_score(
                user_id=user_id,
                item_id=item_id,
                item_type=item_type,
                score=score
            )

            logger.info(
                f"Predict success user={user_id}, item={item_id}, type={item_type}, score={score}"
            )

        except Exception as e:
            logger.error(f"Consumer Error: {e}")