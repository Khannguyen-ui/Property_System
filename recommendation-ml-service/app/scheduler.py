import schedule
import time
import subprocess

from app.utils.logger import get_logger

logger = get_logger(__name__)


def run_train_job():
    logger.info("Start retraining LightGBM model")

    try:
        subprocess.run(
            ["python", "-m", "app.train"],
            check=True
        )

        logger.info("Retraining completed successfully")

    except subprocess.CalledProcessError as e:
        logger.exception(f"Training process failed: {e}")

    except Exception:
        logger.exception("Unexpected error during retraining")


def start_scheduler():
    schedule.every().day.at("02:00").do(run_train_job)

    logger.info("Training scheduler started (every day at 02:00)")

    while True:
        schedule.run_pending()
        time.sleep(60)