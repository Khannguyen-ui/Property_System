import os
import joblib
import lightgbm as lgb
import numpy as np

MODEL_PATH = "app/model.pkl"

fallback_model = lgb.LGBMClassifier(
    n_estimators=20,
    learning_rate=0.1,
    num_leaves=4,
    min_data_in_leaf=1,
    min_data_in_bin=1,
    verbose=-1
)

X_train = np.array([
    [0.1, 0.3, 0, 0, 3000000000],
    [0.3, 0.3, 0, 1, 2000000000],
    [0.6, 0.5, 1, 1, 500000000],
    [0.8, 0.8, 1, 1, 200000000],
    [1.0, 1.0, 1, 1, 0],
    [0.2, 0.8, 0, 0, 4000000000],
    [0.9, 0.3, 1, 0, 2500000000],
    [0.7, 0.9, 1, 1, 100000000],
])

y_train = [0, 0, 1, 1, 1, 0, 0, 1]

fallback_model.fit(X_train, y_train)


def load_model():
    if os.path.exists(MODEL_PATH):
        return joblib.load(MODEL_PATH)

    return fallback_model


model = load_model()


def clamp(value, min_value=0.0, max_value=1.0):
    return max(min_value, min(value, max_value))


def rule_score(features):
    watch_ratio = features[0]
    action_score = features[1]
    location_match = features[2]
    category_match = features[3]
    price_diff = min(features[4], 5000000000)

    price_score = 1.0

    if price_diff > 0:
        price_score = max(
            0.0,
            1.0 - (price_diff / 5000000000)
        )

    score = (
        watch_ratio * 0.25 +
        action_score * 0.35 +
        location_match * 0.15 +
        category_match * 0.15 +
        price_score * 0.10
    )

    return clamp(score)


def predict_score(features):
    if isinstance(features[0], list):
        features = features[0]

    try:
        ml_score = model.predict_proba([features])[0][1]
    except Exception:
        ml_score = 0.5

    rule = rule_score(features)

    final_score = rule * 0.7 + ml_score * 0.3

    return float(clamp(final_score))