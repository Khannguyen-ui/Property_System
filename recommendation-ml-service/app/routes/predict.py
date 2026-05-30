from fastapi import APIRouter, HTTPException

from app.schemas.predict_schema import PredictRequest, PredictResponse
from app.feature import extract_features
from app.model import predict_score
from app.redis_client import save_score
from app.utils.logger import get_logger

router = APIRouter(prefix="/ml", tags=["Prediction"])

logger = get_logger(__name__)


@router.post("/predict", response_model=PredictResponse)
def predict(request: PredictRequest):
    try:
        event = request.dict()

        features = extract_features(event)
        score = predict_score(features)

        try:
            save_score(
                user_id=request.userId,
                item_id=request.itemId,
                item_type=request.itemType,
                score=score
            )
        except Exception as redis_error:
            logger.error(f"Redis save error: {redis_error}")

        if score >= 0.7:
            label = "high_interest"
        elif score >= 0.4:
            label = "medium_interest"
        else:
            label = "low_interest"

        return PredictResponse(
            userId=request.userId,
            itemId=request.itemId,
            itemType=request.itemType,
            score=score,
            label=label
        )

    except Exception as e:
        logger.error(f"Predict error: {e}")
        raise HTTPException(status_code=500, detail=str(e))