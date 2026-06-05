import os
import joblib
import pandas as pd
import lightgbm as lgb
import psycopg2

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
            location_match,
            category_match,
            score
        FROM user_behavior
    """

    df = pd.read_sql(query, conn)

    conn.close()

    return df


def build_features(df):
    df["action"] = df["action"].fillna("VIEW").str.upper()

    df["watch_time"] = df["watch_time"].fillna(0)
    df["duration"] = df["duration"].fillna(1)
    df["price"] = df["price"].fillna(0)
    df["user_budget"] = df["user_budget"].fillna(0)
    df["location_match"] = df["location_match"].fillna(0)
    df["category_match"] = df["category_match"].fillna(0)

    df["watch_ratio"] = df["watch_time"] / df["duration"].replace(0, 1)

    df["action_score"] = df["action"].map(ACTION_WEIGHT).fillna(0.1)

    df["price_diff"] = abs(df["price"] - df["user_budget"])

    X = df[
        [
            "watch_ratio",
            "action_score",
            "location_match",
            "category_match",
            "price_diff"
        ]
    ]

    y = (df["score"] >= 0.6).astype(int)

    return X, y


def train():
    df = load_data()

    if df.empty:
        print("No behavior data found")
        return

    X, y = build_features(df)

    model = lgb.LGBMClassifier(
        n_estimators=50,
        learning_rate=0.05,
        num_leaves=8,
        min_data_in_leaf=1,
        min_data_in_bin=1,
        verbose=-1
    )

    model.fit(X, y)

    joblib.dump(model, "app/model.pkl")

    print("Model trained and saved to app/model.pkl")


if __name__ == "__main__":
    train()