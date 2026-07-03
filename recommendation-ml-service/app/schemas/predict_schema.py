from pydantic import BaseModel, Field
from typing import Optional, List


class PredictRequest(BaseModel):
    userId: int = Field(..., gt=0)
    itemId: int = Field(..., gt=0)
    itemType: str
    action: str

    watchTime: Optional[float] = Field(default=0, ge=0)
    duration: Optional[float] = Field(default=1, gt=0)

    price: Optional[float] = Field(default=0, ge=0)
    userBudget: Optional[float] = Field(default=0, ge=0)

    area: Optional[float] = Field(default=0, ge=0)
    userArea: Optional[float] = Field(default=0, ge=0)

    province: Optional[str] = ""
    district: Optional[str] = ""
    ward: Optional[str] = ""
    street: Optional[str] = ""

    userProvince: Optional[str] = ""
    userDistrict: Optional[str] = ""
    userWard: Optional[str] = ""
    userStreet: Optional[str] = ""

    propertyType: Optional[str] = ""
    userPropertyType: Optional[str] = ""

    transactionType: Optional[str] = ""
    userTransactionType: Optional[str] = ""

    bedrooms: Optional[int] = Field(default=0, ge=0)
    userBedrooms: Optional[int] = Field(default=0, ge=0)

    bathrooms: Optional[int] = Field(default=0, ge=0)
    userBathrooms: Optional[int] = Field(default=0, ge=0)

    hasBalcony: Optional[bool] = False
    userHasBalcony: Optional[bool] = False

    furnishingStatus: Optional[str] = ""
    userFurnishingStatus: Optional[str] = ""

    availabilityStatus: Optional[str] = ""
    userAvailabilityStatus: Optional[str] = ""

    amenities: Optional[List[str]] = []
    userAmenities: Optional[List[str]] = []


class PredictResponse(BaseModel):
    userId: int
    itemId: int
    itemType: str
    score: float
    label: str