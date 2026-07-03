ACTION_WEIGHT = {
    "VIEW": 0.3,
    "CLICK": 0.5,
    "LIKE": 0.8,
    "SAVE": 0.9,
    "COMMENT": 0.95,
    "FOLLOW_OWNER": 0.98,
    "CONTACT": 1.0,
    "SHARE": 0.7
}


def safe_str(value):
    return str(value or "").strip().lower()


def match_text(a, b):
    a = safe_str(a)
    b = safe_str(b)

    if not a or not b:
        return 0

    return 1 if a == b else 0


def match_number(a, b):
    try:
        a = int(a or 0)
        b = int(b or 0)

        if a <= 0 or b <= 0:
            return 0

        return 1 if a == b else 0
    except Exception:
        return 0


def match_bool(a, b):
    if a is None or b is None:
        return 0

    return 1 if bool(a) == bool(b) else 0


def diff_ratio(value, target):
    try:
        value = float(value or 0)
        target = float(target or 0)

        base = target if target > 0 else value
        if base <= 0:
            return 1.0

        return min(abs(value - target) / base, 1.0)
    except Exception:
        return 1.0


def amenity_ratio(amenities, user_amenities):
    amenities = set([safe_str(x) for x in (amenities or []) if safe_str(x)])
    user_amenities = set([safe_str(x) for x in (user_amenities or []) if safe_str(x)])

    if not amenities or not user_amenities:
        return 0.0

    return len(amenities.intersection(user_amenities)) / len(user_amenities)


def extract_features(event):
    action = safe_str(event.get("action")).upper()

    watch_time = float(event.get("watchTime") or 0)
    duration = float(event.get("duration") or 1)

    if duration <= 0:
        duration = 1

    watch_ratio = min(watch_time / duration, 1.0)

    action_score = ACTION_WEIGHT.get(action, 0.1)

    province_match = match_text(event.get("province"), event.get("userProvince"))
    district_match = match_text(event.get("district"), event.get("userDistrict"))
    ward_match = match_text(event.get("ward"), event.get("userWard"))
    street_match = match_text(event.get("street"), event.get("userStreet"))

    category_match = match_text(event.get("propertyType"), event.get("userPropertyType"))
    transaction_match = match_text(event.get("transactionType"), event.get("userTransactionType"))

    price_diff_ratio = diff_ratio(event.get("price"), event.get("userBudget"))
    area_diff_ratio = diff_ratio(event.get("area"), event.get("userArea"))

    bedroom_match = match_number(event.get("bedrooms"), event.get("userBedrooms"))
    bathroom_match = match_number(event.get("bathrooms"), event.get("userBathrooms"))
    balcony_match = match_bool(event.get("hasBalcony"), event.get("userHasBalcony"))

    furnishing_match = match_text(
        event.get("furnishingStatus"),
        event.get("userFurnishingStatus")
    )

    availability_match = match_text(
        event.get("availabilityStatus"),
        event.get("userAvailabilityStatus")
    )

    amenity_match_ratio = amenity_ratio(
        event.get("amenities"),
        event.get("userAmenities")
    )

    return [
        watch_ratio,
        action_score,

        province_match,
        district_match,
        ward_match,
        street_match,

        category_match,
        transaction_match,

        price_diff_ratio,
        area_diff_ratio,

        bedroom_match,
        bathroom_match,
        balcony_match,
        furnishing_match,
        availability_match,

        amenity_match_ratio,
    ]