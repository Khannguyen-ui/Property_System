import os
import json
from kafka import KafkaConsumer
from kafka.errors import KafkaError

from app.feature import extract_features
from app.model import predict_score
from app.redis_client import save_score
from app.utils.logger import get_logger

logger = get_logger(__name__)

KAFKA_BOOTSTRAP_SERVERS = os.getenv(
    "KAFKA_BOOTSTRAP_SERVERS",
    "localhost:9092"
)

KAFKA_TOPIC = os.getenv("KAFKA_TOPIC", "user-behavior")
KAFKA_GROUP_ID = os.getenv("KAFKA_GROUP_ID", "recommendation-ml-group")


def deserialize_message(value):
    try:
        return json.loads(value.decode("utf-8"))
    except Exception as e:
        logger.warning(f"Invalid Kafka message format: {e}")
        return None


def is_valid_event(event):
    if not event:
        return False

    required_fields = ["userId", "itemId", "action"]

    for field in required_fields:
        if field not in event or event[field] is None:
            logger.warning(f"Missing required field: {field}, event={event}")
            return False

    return True


def handle_event(event):
    if not is_valid_event(event):
        return

    user_id = event["userId"]
    item_id = event["itemId"]
    item_type = event.get("itemType", "PROPERTY")

    features = extract_features(event)
    score = float(predict_score(features))

    try:
        save_score(
            user_id=user_id,
            item_id=item_id,
            item_type=item_type,
            score=score
        )
    except Exception as e:
        logger.warning(
            f"Redis save failed user={user_id}, item={item_id}: {e}"
        )

    logger.info(
        f"Predict success user={user_id}, item={item_id}, "
        f"type={item_type}, action={event.get('action')}, score={score:.4f}"
    )


def start_consumer():
    try:
        logger.info(f"Connecting Kafka: {KAFKA_BOOTSTRAP_SERVERS}")

        consumer = KafkaConsumer(
            KAFKA_TOPIC,
            bootstrap_servers=KAFKA_BOOTSTRAP_SERVERS,
            value_deserializer=deserialize_message,
            auto_offset_reset="earliest",
            enable_auto_commit=True,
            group_id=KAFKA_GROUP_ID,
            consumer_timeout_ms=0
        )

        logger.info(
            f"Kafka Consumer started topic={KAFKA_TOPIC}, group={KAFKA_GROUP_ID}"
        )

        for message in consumer:
            event = message.value

            try:
                logger.info(
                    f"Received event topic={message.topic}, "
                    f"partition={message.partition}, offset={message.offset}"
                )

                handle_event(event)

            except Exception:
                logger.exception(
                    f"Consumer event processing failed, event={event}"
                )

    except KafkaError:
        logger.exception("Kafka connection error")

    except Exception:
        logger.exception("Consumer fatal error")