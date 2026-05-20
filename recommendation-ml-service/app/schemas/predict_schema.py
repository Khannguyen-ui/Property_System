from pydantic import BaseModel, Field
from typing import Optional


class PredictRequest(BaseModel):

    userId: int = Field(..., gt=0)

    itemId: int = Field(..., gt=0)

    itemType: str

    action: str

    watchTime: Optional[float] = Field(default=0, ge=0)

    duration: Optional[float] = Field(default=1, gt=0)

    price: Optional[float] = Field(default=0, ge=0)

    userBudget: Optional[float] = Field(default=0, ge=0)

    locationMatch: Optional[int] = Field(default=0, ge=0, le=1)

    categoryMatch: Optional[int] = Field(default=0, ge=0, le=1)


class PredictResponse(BaseModel):

    userId: int

    itemId: int

    itemType: str

    score: float

    label: str