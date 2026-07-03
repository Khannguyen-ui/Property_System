import os
import redis

from app.utils.logger import get_logger

logger = get_logger(__name__)

REDIS_HOST = os.getenv("REDIS_HOST", "localhost")
REDIS_PORT = int(os.getenv("REDIS_PORT", 6379))
REDIS_DB = int(os.getenv("REDIS_DB", 0))

CACHE_EXPIRE = 60 * 60 * 24 * 7  # 7 ngày


redis_client = redis.Redis(
    host=REDIS_HOST,
    port=REDIS_PORT,
    db=REDIS_DB,
    decode_responses=True,
    socket_timeout=5,
    socket_connect_timeout=5,
    health_check_interval=30,
)


def check_connection():
    try:
        redis_client.ping()
        logger.info(
            f"Connected Redis {REDIS_HOST}:{REDIS_PORT}"
        )
    except Exception:
        logger.exception("Cannot connect Redis")


def get_recommendation_key(user_id: int, item_type: str) -> str:
    item_type = str(item_type).strip().lower()

    if item_type == "reel":
        return f"user:{user_id}:recommend:reels"

    return f"user:{user_id}:recommend:properties"


def save_score(
    user_id: int,
    item_id: int,
    item_type: str,
    score: float,
):
    key = get_recommendation_key(user_id, item_type)

    try:
        redis_client.zadd(
            key,
            {
                str(item_id): float(score)
            }
        )

        redis_client.expire(
            key,
            CACHE_EXPIRE
        )

        logger.info(
            f"Saved recommendation "
            f"user={user_id}, "
            f"item={item_id}, "
            f"type={item_type}, "
            f"score={score:.4f}"
        )

    except Exception:
        logger.exception(
            f"Failed to save recommendation "
            f"user={user_id}, "
            f"item={item_id}"
        )


def get_scores(user_id: int, item_type: str):
    key = get_recommendation_key(user_id, item_type)

    try:
        return redis_client.zrevrange(
            key,
            0,
            -1,
            withscores=True
        )
    except Exception:
        logger.exception(
            f"Cannot load recommendation user={user_id}"
        )
        return []


check_connection()