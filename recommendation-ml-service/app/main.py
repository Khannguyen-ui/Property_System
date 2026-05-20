from fastapi import FastAPI
import threading

from app.routes.predict import router as predict_router
from app.consumer import start_consumer
from app.scheduler import start_scheduler
from app.utils.logger import get_logger

logger = get_logger(__name__)

app = FastAPI(
    title="Recommendation ML Service"
)

app.include_router(predict_router)


@app.on_event("startup")
def startup():
    consumer_thread = threading.Thread(
        target=start_consumer,
        daemon=True
    )

    scheduler_thread = threading.Thread(
        target=start_scheduler,
        daemon=True
    )

    consumer_thread.start()
    scheduler_thread.start()

    logger.info("ML service started")


@app.get("/")
def root():
    return {
        "status": "ML service running"
    }