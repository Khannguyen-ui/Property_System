package com.homeverse.recommendation.repository;

import com.homeverse.recommendation.model.UserBehavior;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import com.homeverse.recommendation.dto.TrendingItemProjection;
import com.homeverse.recommendation.dto.CollaborativeItemProjection;
import java.util.List;

public interface UserBehaviorRepository extends JpaRepository<UserBehavior, Long> {

    long countByActionIgnoreCase(String action);

    @Query("SELECT COALESCE(AVG(u.score), 0) FROM UserBehavior u")
    Double avgScore();

    @Query("""
                SELECT u.itemType
                FROM UserBehavior u
                GROUP BY u.itemType
                ORDER BY COUNT(u.itemType) DESC
                LIMIT 1
            """)
    String findTopItemType();

    @Query("""
                SELECT u.action
                FROM UserBehavior u
                GROUP BY u.action
                ORDER BY COUNT(u.action) DESC
                LIMIT 1
            """)
    String findTopAction();

    @Query(value = """
                SELECT item_type
                FROM user_behavior
                WHERE user_id = :userId
                GROUP BY item_type
                ORDER BY COUNT(*) DESC
                LIMIT 1
            """, nativeQuery = true)
    String findFavoriteItemTypeByUserId(Long userId);

    @Query(value = """
                SELECT action
                FROM user_behavior
                WHERE user_id = :userId
                GROUP BY action
                ORDER BY COUNT(*) DESC
                LIMIT 1
            """, nativeQuery = true)
    String findFavoriteActionByUserId(Long userId);

    @Query("""
                SELECT COALESCE(AVG(u.score), 0)
                FROM UserBehavior u
                WHERE u.userId = :userId
            """)
    Double avgScoreByUserId(Long userId);

    @Query("""
                SELECT COALESCE(AVG(u.userBudget), 0)
                FROM UserBehavior u
                WHERE u.userId = :userId
            """)
    Double avgBudgetByUserId(Long userId);

    @Query(value = """
                SELECT
                    item_id AS itemId,
                    item_type AS itemType,
                    SUM(
                        CASE
                            WHEN UPPER(action) = 'VIEW' THEN 0.2
                            WHEN UPPER(action) = 'CLICK' THEN 0.4
                            WHEN UPPER(action) = 'LIKE' THEN 0.7
                            WHEN UPPER(action) = 'SAVE' THEN 0.9
                            WHEN UPPER(action) = 'CONTACT' THEN 1.2
                            ELSE 0.1
                        END
                    ) + AVG(score) AS trendingScore
                FROM user_behavior
                WHERE item_type = 'property'
                GROUP BY item_id, item_type
                ORDER BY trendingScore DESC
                LIMIT 20
            """, nativeQuery = true)
    List<TrendingItemProjection> findTrendingProperties();

    @Query(value = """
                SELECT
                    item_id AS itemId,
                    item_type AS itemType,
                    SUM(
                        CASE
                            WHEN UPPER(action) = 'VIEW' THEN 0.2
                            WHEN UPPER(action) = 'CLICK' THEN 0.4
                            WHEN UPPER(action) = 'LIKE' THEN 0.7
                            WHEN UPPER(action) = 'SAVE' THEN 0.9
                            WHEN UPPER(action) = 'CONTACT' THEN 1.2
                            ELSE 0.1
                        END
                    ) + AVG(score) AS trendingScore
                FROM user_behavior
                WHERE item_type = 'reel'
                GROUP BY item_id, item_type
                ORDER BY trendingScore DESC
                LIMIT 20
            """, nativeQuery = true)
    List<TrendingItemProjection> findTrendingReels();
@Query("""
    SELECT u.province
    FROM UserBehavior u
    WHERE u.userId = :userId
      AND u.province IS NOT NULL
      AND u.province <> ''
    GROUP BY u.province
    ORDER BY COUNT(u.province) DESC
    LIMIT 1
""")
String findFavoriteProvinceByUserId(Long userId);

@Query("""
    SELECT u.ward
    FROM UserBehavior u
    WHERE u.userId = :userId
      AND u.ward IS NOT NULL
      AND u.ward <> ''
    GROUP BY u.ward
    ORDER BY COUNT(u.ward) DESC
    LIMIT 1
""")
String findFavoriteWardByUserId(Long userId);

@Query("""
    SELECT u.street
    FROM UserBehavior u
    WHERE u.userId = :userId
      AND u.street IS NOT NULL
      AND u.street <> ''
    GROUP BY u.street
    ORDER BY COUNT(u.street) DESC
    LIMIT 1
""")
String findFavoriteStreetByUserId(Long userId);

@Query("""
    SELECT u.propertyType
    FROM UserBehavior u
    WHERE u.userId = :userId
      AND u.propertyType IS NOT NULL
      AND u.propertyType <> ''
    GROUP BY u.propertyType
    ORDER BY COUNT(u.propertyType) DESC
    LIMIT 1
""")
String findFavoritePropertyTypeByUserId(Long userId);

@Query("""
    SELECT u.transactionType
    FROM UserBehavior u
    WHERE u.userId = :userId
      AND u.transactionType IS NOT NULL
      AND u.transactionType <> ''
    GROUP BY u.transactionType
    ORDER BY COUNT(u.transactionType) DESC
    LIMIT 1
""")
String findFavoriteTransactionTypeByUserId(Long userId);

@Query("""
    SELECT COALESCE(AVG(u.area), 0)
    FROM UserBehavior u
    WHERE u.userId = :userId
      AND u.area IS NOT NULL
      AND u.area > 0
""")
Double avgAreaByUserId(Long userId);
    @Query(value = """
                WITH current_user_likes AS (
                    SELECT DISTINCT item_id
                    FROM user_behavior
                    WHERE user_id = :userId
                      AND UPPER(action) IN ('LIKE', 'SAVE', 'CONTACT')
                ),
                similar_users AS (
                    SELECT
                        ub.user_id,
                        COUNT(*) AS common_count
                    FROM user_behavior ub
                    JOIN current_user_likes cul
                        ON ub.item_id = cul.item_id
                    WHERE ub.user_id <> :userId
                      AND UPPER(ub.action) IN ('LIKE', 'SAVE', 'CONTACT')
                    GROUP BY ub.user_id
                    ORDER BY common_count DESC
                    LIMIT 20
                ),
                current_user_seen AS (
                    SELECT DISTINCT item_id
                    FROM user_behavior
                    WHERE user_id = :userId
                )
                SELECT
                    ub.item_id AS itemId,
                    ub.item_type AS itemType,
                    SUM(
                        CASE
                            WHEN UPPER(ub.action) = 'LIKE' THEN 1.0
                            WHEN UPPER(ub.action) = 'SAVE' THEN 1.3
                            WHEN UPPER(ub.action) = 'CONTACT' THEN 1.6
                            ELSE 0.2
                        END
                    ) AS cfScore
                FROM user_behavior ub
                JOIN similar_users su
                    ON ub.user_id = su.user_id
                LEFT JOIN current_user_seen seen
                    ON ub.item_id = seen.item_id
                WHERE seen.item_id IS NULL
                  AND ub.item_type = 'property'
                  AND UPPER(ub.action) IN ('LIKE', 'SAVE', 'CONTACT')
                GROUP BY ub.item_id, ub.item_type
                ORDER BY cfScore DESC
                LIMIT 20
            """, nativeQuery = true)
    List<CollaborativeItemProjection> findCollaborativeProperties(Long userId);

    @Query(value = """
                WITH current_user_likes AS (
                    SELECT DISTINCT item_id
                    FROM user_behavior
                    WHERE user_id = :userId
                      AND UPPER(action) IN ('LIKE', 'SAVE', 'CONTACT')
                ),
                similar_users AS (
                    SELECT
                        ub.user_id,
                        COUNT(*) AS common_count
                    FROM user_behavior ub
                    JOIN current_user_likes cul
                        ON ub.item_id = cul.item_id
                    WHERE ub.user_id <> :userId
                      AND UPPER(ub.action) IN ('LIKE', 'SAVE', 'CONTACT')
                    GROUP BY ub.user_id
                    ORDER BY common_count DESC
                    LIMIT 20
                ),
                current_user_seen AS (
                    SELECT DISTINCT item_id
                    FROM user_behavior
                    WHERE user_id = :userId
                )
                SELECT
                    ub.item_id AS itemId,
                    ub.item_type AS itemType,
                    SUM(
                        CASE
                            WHEN UPPER(ub.action) = 'LIKE' THEN 1.0
                            WHEN UPPER(ub.action) = 'SAVE' THEN 1.3
                            WHEN UPPER(ub.action) = 'CONTACT' THEN 1.6
                            ELSE 0.2
                        END
                    ) AS cfScore
                FROM user_behavior ub
                JOIN similar_users su
                    ON ub.user_id = su.user_id
                LEFT JOIN current_user_seen seen
                    ON ub.item_id = seen.item_id
                WHERE seen.item_id IS NULL
                  AND ub.item_type = 'reel'
                  AND UPPER(ub.action) IN ('LIKE', 'SAVE', 'CONTACT')
                GROUP BY ub.item_id, ub.item_type
                ORDER BY cfScore DESC
                LIMIT 20
            """, nativeQuery = true)
    List<CollaborativeItemProjection> findCollaborativeReels(Long userId);
}