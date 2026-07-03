import os
import joblib
import lightgbm as lgb
import numpy as np

MODEL_PATH = "app/model.pkl"

FEATURE_COLUMNS = [
    "watch_ratio",
    "action_score",

    "province_match",
    "district_match",
    "ward_match",
    "street_match",

    "category_match",
    "transaction_match",

    "price_diff_ratio",
    "area_diff_ratio",

    "bedroom_match",
    "bathroom_match",
    "balcony_match",
    "furnishing_match",
    "availability_match",

    "amenity_match_ratio",
]

fallback_model = lgb.LGBMClassifier(
    n_estimators=30,
    learning_rate=0.08,
    num_leaves=8,
    min_data_in_leaf=1,
    min_data_in_bin=1,
    verbose=-1,
)

X_train = np.array([
    # watch, action, province, district, ward, street, category, transaction,
    # price, area, bedroom, bathroom, balcony, furnishing, availability, amenity

    [0.10, 0.30, 0, 0, 0, 0, 0, 1, 1.00, 1.00, 0, 0, 0, 0, 0, 0.00],
    [0.25, 0.30, 1, 0, 0, 0, 0, 1, 0.80, 0.70, 0, 0, 0, 0, 0, 0.10],
    [0.40, 0.50, 1, 1, 0, 0, 1, 1, 0.60, 0.50, 0, 0, 1, 0, 0, 0.30],
    [0.65, 0.80, 1, 1, 1, 0, 1, 1, 0.35, 0.30, 1, 1, 1, 1, 0, 0.55],
    [0.80, 0.90, 1, 1, 1, 1, 1, 1, 0.20, 0.20, 1, 1, 1, 1, 1, 0.75],
    [1.00, 1.00, 1, 1, 1, 1, 1, 1, 0.00, 0.00, 1, 1, 1, 1, 1, 1.00],

    [0.20, 0.80, 0, 0, 0, 0, 0, 0, 1.00, 0.90, 0, 0, 0, 0, 0, 0.05],
    [0.90, 0.30, 1, 0, 0, 0, 1, 1, 0.70, 0.60, 0, 0, 0, 1, 0, 0.20],
    [0.70, 0.95, 1, 1, 0, 0, 1, 1, 0.25, 0.40, 1, 1, 0, 1, 1, 0.60],
    [0.95, 0.98, 1, 1, 1, 0, 1, 1, 0.10, 0.10, 1, 1, 1, 1, 1, 0.85],
])

y_train = [0, 0, 0, 1, 1, 1, 0, 0, 1, 1]

fallback_model.fit(X_train, y_train)


def load_model():
    if os.path.exists(MODEL_PATH):
        return joblib.load(MODEL_PATH)

    return fallback_model


model = load_model()


def clamp(value, min_value=0.0, max_value=1.0):
    return max(min_value, min(float(value), max_value))


def normalize_features(features):
    if isinstance(features, np.ndarray):
        features = features.tolist()

    if isinstance(features[0], list):
        features = features[0]

    features = list(features)

    if len(features) < len(FEATURE_COLUMNS):
        features += [0.0] * (len(FEATURE_COLUMNS) - len(features))

    features = features[:len(FEATURE_COLUMNS)]

    return [clamp(v) for v in features]


def rule_score(features):
    data = dict(zip(FEATURE_COLUMNS, normalize_features(features)))

    location_score = (
        data["province_match"] * 0.20 +
        data["district_match"] * 0.35 +
        data["ward_match"] * 0.30 +
        data["street_match"] * 0.15
    )

    price_score = 1.0 - data["price_diff_ratio"]
    area_score = 1.0 - data["area_diff_ratio"]

    property_score = (
        data["category_match"] * 0.20 +
        data["transaction_match"] * 0.18 +
        data["bedroom_match"] * 0.12 +
        data["bathroom_match"] * 0.08 +
        data["balcony_match"] * 0.06 +
        data["furnishing_match"] * 0.10 +
        data["availability_match"] * 0.08 +
        data["amenity_match_ratio"] * 0.18
    )

    score = (
        data["watch_ratio"] * 0.15 +
        data["action_score"] * 0.25 +
        location_score * 0.25 +
        price_score * 0.12 +
        area_score * 0.08 +
        property_score * 0.15
    )

    return clamp(score)


def predict_score(features):
    features = normalize_features(features)

    try:
        ml_score = model.predict_proba([features])[0][1]
    except Exception:
        ml_score = 0.5

    rule = rule_score(features)

    final_score = rule * 0.7 + ml_score * 0.3

    return float(clamp(final_score))