import os
import redis

from app.utils.logger import get_logger

logger = get_logger(__name__)

REDIS_HOST = os.getenv("REDIS_HOST", "localhost")

REDIS_PORT = int(
    os.getenv("REDIS_PORT", 6379)
)

r = redis.Redis(
    host=REDIS_HOST,
    port=REDIS_PORT,
    decode_responses=True
)


def save_score(user_id, item_id, item_type, score):

    item_type = str(item_type).lower()

    if item_type == "reel":
        key = f"user:{user_id}:recommend:reels"
    else:
        key = f"user:{user_id}:recommend:properties"

    try:

        r.zadd(
            key,
            {
                str(item_id): float(score)
            }
        )

        r.expire(
            key,
            60 * 60 * 24 * 7
        )

        logger.info(
            f"Saved score user={user_id}, item={item_id}, type={item_type}, score={score}"
        )

    except Exception as e:

        logger.error(
            f"Redis save error: {e}"
        )