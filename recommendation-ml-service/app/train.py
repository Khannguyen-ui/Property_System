import os
import joblib
import pandas as pd
import lightgbm as lgb
import psycopg2

MODEL_PATH = "app/model.pkl"

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


def get_connection():
    return psycopg2.connect(
        host=os.getenv("POSTGRES_HOST", "localhost"),
        port=os.getenv("POSTGRES_PORT", "5432"),
        database=os.getenv("POSTGRES_DB", "recommend_db"),
        user=os.getenv("POSTGRES_USER", "postgres"),
        password=os.getenv("POSTGRES_PASSWORD", "123456")
    )


def load_data():
    conn = get_connection()

    query = """
        SELECT
            user_id,
            item_id,
            item_type,
            action,
            watch_time,
            duration,

            price,
            user_budget,
            area,
            user_area,

            province_match,
            district_match,
            ward_match,
            street_match,

            category_match,
            transaction_match,

            bedroom_match,
            bathroom_match,
            balcony_match,
            furnishing_match,
            availability_match,

            amenity_match_ratio,

            score
        FROM user_behavior
    """

    df = pd.read_sql(query, conn)

    conn.close()

    return df


def ensure_columns(df):
    default_values = {
        "action": "VIEW",
        "watch_time": 0,
        "duration": 1,

        "price": 0,
        "user_budget": 0,
        "area": 0,
        "user_area": 0,

        "province_match": 0,
        "district_match": 0,
        "ward_match": 0,
        "street_match": 0,

        "category_match": 0,
        "transaction_match": 0,

        "bedroom_match": 0,
        "bathroom_match": 0,
        "balcony_match": 0,
        "furnishing_match": 0,
        "availability_match": 0,

        "amenity_match_ratio": 0,
        "score": 0,
    }

    for column, default in default_values.items():
        if column not in df.columns:
            df[column] = default

    return df


def build_features(df):
    df = ensure_columns(df)

    df["action"] = df["action"].fillna("VIEW").astype(str).str.upper()

    numeric_columns = [
        "watch_time",
        "duration",
        "price",
        "user_budget",
        "area",
        "user_area",

        "province_match",
        "district_match",
        "ward_match",
        "street_match",

        "category_match",
        "transaction_match",

        "bedroom_match",
        "bathroom_match",
        "balcony_match",
        "furnishing_match",
        "availability_match",

        "amenity_match_ratio",
        "score",
    ]

    for column in numeric_columns:
        df[column] = pd.to_numeric(df[column], errors="coerce").fillna(0)

    df["duration"] = df["duration"].replace(0, 1)

    df["watch_ratio"] = (
        df["watch_time"] / df["duration"]
    ).clip(0, 1)

    df["action_score"] = (
        df["action"]
        .map(ACTION_WEIGHT)
        .fillna(0.1)
    )

    budget_base = df["user_budget"].copy()
    budget_base = budget_base.where(budget_base > 0, df["price"])
    budget_base = budget_base.where(budget_base > 0, 1)

    df["price_diff_ratio"] = (
        (df["price"] - df["user_budget"]).abs() / budget_base
    ).clip(0, 1)

    area_base = df["user_area"].copy()
    area_base = area_base.where(area_base > 0, df["area"])
    area_base = area_base.where(area_base > 0, 1)

    df["area_diff_ratio"] = (
        (df["area"] - df["user_area"]).abs() / area_base
    ).clip(0, 1)

    for column in [
        "province_match",
        "district_match",
        "ward_match",
        "street_match",
        "category_match",
        "transaction_match",
        "bedroom_match",
        "bathroom_match",
        "balcony_match",
        "furnishing_match",
        "availability_match",
        "amenity_match_ratio",
    ]:
        df[column] = df[column].clip(0, 1)

    X = df[FEATURE_COLUMNS]

    y = (df["score"] >= 0.6).astype(int)

    return X, y


def train():
    df = load_data()

    if df.empty:
        print("No behavior data found")
        return

    X, y = build_features(df)

    if y.nunique() < 2:
        print("Not enough label variety to train model")
        return

    model = lgb.LGBMClassifier(
        n_estimators=80,
        learning_rate=0.05,
        num_leaves=12,
        min_data_in_leaf=1,
        min_data_in_bin=1,
        verbose=-1
    )

    model.fit(X, y)

    os.makedirs(os.path.dirname(MODEL_PATH), exist_ok=True)
    joblib.dump(model, MODEL_PATH)

    print(f"Model trained and saved to {MODEL_PATH}")
    print(f"Features used: {FEATURE_COLUMNS}")


if __name__ == "__main__":
    train()