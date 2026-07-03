import threading
from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.routes.predict import router as predict_router
from app.consumer import start_consumer
from app.scheduler import start_scheduler
from app.utils.logger import get_logger

logger = get_logger(__name__)


def run_background_task(name, target):
    thread = threading.Thread(
        target=target,
        name=name,
        daemon=True
    )

    thread.start()

    logger.info(f"{name} started")

    return thread


@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("Starting Recommendation ML Service")

    run_background_task(
        name="kafka-consumer-thread",
        target=start_consumer
    )

    run_background_task(
        name="training-scheduler-thread",
        target=start_scheduler
    )

    logger.info("Recommendation ML Service started")

    yield

    logger.info("Recommendation ML Service stopped")


app = FastAPI(
    title="Recommendation ML Service",
    version="1.0.0",
    description="AI Recommendation service using LightGBM, Kafka and Redis",
    lifespan=lifespan
)

app.include_router(predict_router)


@app.get("/")
def root():
    return {
        "status": "running",
        "service": "recommendation-ml-service",
        "version": "1.0.0"
    }


@app.get("/health")
def health_check():
    return {
        "status": "UP"
    }