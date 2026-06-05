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


def extract_features(event):

    action = str(
        event.get("action", "VIEW")
    ).upper()

    watch_time = float(
        event.get("watchTime", 0)
    )

    duration = float(
        event.get("duration", 1)
    )

    watch_ratio = 0.0

    if duration > 0:
        watch_ratio = watch_time / duration

    action_score = ACTION_WEIGHT.get(
        action,
        0.1
    )

    price = float(
        event.get("price", 0)
    )

    user_budget = float(
        event.get("userBudget", 0)
    )

    price_diff = abs(price - user_budget)

    location_match = int(
        event.get("locationMatch", 0)
    )

    category_match = int(
        event.get("categoryMatch", 0)
    )

    return [[
        watch_ratio,
        action_score,
        location_match,
        category_match,
        price_diff
    ]]