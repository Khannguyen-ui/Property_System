import schedule
import time
import subprocess

from app.utils.logger import get_logger

logger = get_logger(__name__)


def run_train_job():
    try:
        logger.info("Start retraining model")

        subprocess.run(
            ["python", "-m", "app.train"],
            check=True
        )

        logger.info("Retraining model completed")

    except Exception as e:
        logger.error(f"Retraining model failed: {e}")


def start_scheduler():
    schedule.every().day.at("02:00").do(run_train_job)

    logger.info("Training scheduler started")

    while True:
        schedule.run_pending()
        time.sleep(60)