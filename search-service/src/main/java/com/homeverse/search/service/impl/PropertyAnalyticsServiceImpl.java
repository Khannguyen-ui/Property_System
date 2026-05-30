package com.homeverse.search.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval;
import co.elastic.clients.elasticsearch._types.aggregations.DateHistogramBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.homeverse.search.dto.response.*;
import com.homeverse.search.service.PropertyAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PropertyAnalyticsServiceImpl implements PropertyAnalyticsService {

    private final ElasticsearchClient esClient;

    @Value("${elasticsearch.index.properties:properties_v1}")
    private String indexName;

    @Override
    public PropertyAnalyticsResponse getPriceTrends(String province, String district, String ward, String propertyType, String transactionType) {
        try {
            // 1. Khởi tạo Query lọc dữ liệu
            BoolQuery.Builder boolQuery = new BoolQuery.Builder()
                    .filter(f -> f.term(t -> t.field("status").value("ACTIVE")))
                    .filter(f -> f.term(t -> t.field("transactionType").value(transactionType)));

            if (province != null && !province.isEmpty()) {
                boolQuery.filter(f -> f.term(t -> t.field("province.keyword").value(province)));
            }
            if (district != null && !district.isEmpty()) {
                boolQuery.filter(f -> f.term(t -> t.field("district.keyword").value(district))); // 🟢 THÊM .keyword
            }
            if (ward != null && !ward.isEmpty()) {
                boolQuery.filter(f -> f.term(t -> t.field("ward.keyword").value(ward))); // 🟢 THÊM .keyword
            }
            if (propertyType != null && !propertyType.isEmpty()) {
                boolQuery.filter(f -> f.term(t -> t.field("propertyType").value(propertyType)));
            }

            // Chọn trường tính toán: Bán dùng pricePerSqm, Thuê dùng price
            String targetField = "FOR_SALE".equals(transactionType) ? "pricePerSqm" : "price";

            // 2. Build Aggregation (Sử dụng Percentiles để lấy giá phổ biến - Median)
            SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index(indexName)
                    .size(0)
                    .query(q -> q.bool(boolQuery.build()))
                    .aggregations("price_trends_by_month", a -> a
                            .dateHistogram(h -> h.field("createdAt").calendarInterval(CalendarInterval.Month).format("MM/yyyy"))
                            .aggregations("avg_price", sub -> sub.avg(avg -> avg.field(targetField)))
                            .aggregations("popular_price", sub -> sub.percentiles(p -> p.field(targetField).percents(50.0)))
                    )
            );

            SearchResponse<Void> response = esClient.search(searchRequest, Void.class);
            List<PriceTrendDTO> trends = new ArrayList<>();
            Aggregate aggregate = response.aggregations().get("price_trends_by_month");

            if (aggregate != null && aggregate.isDateHistogram()) {
                for (DateHistogramBucket bucket : aggregate.dateHistogram().buckets().array()) {
                    // Lấy giá trị Median (50th percentile)
                    double medianVal = 0.0;
                    Aggregate popAgg = bucket.aggregations().get("popular_price");

                    // Thư viện ES v8 yêu cầu phải gọi qua tdigestPercentiles và dạng keyed()
                    if (popAgg != null && popAgg.isTdigestPercentiles()) {
                        String valStr = popAgg.tdigestPercentiles().values().keyed().get("50.0");
                        if (valStr != null && !valStr.equals("NaN")) {
                            medianVal = Double.parseDouble(valStr); // Ép kiểu từ String sang Double
                        }
                    }

                    if (Double.isNaN(medianVal)) {
                        medianVal = 0.0;
                    }

                    trends.add(PriceTrendDTO.builder()
                            .month(bucket.keyAsString())
                            .averagePrice(BigDecimal.valueOf(medianVal).setScale(0, RoundingMode.HALF_UP))
                            .totalPosts(bucket.docCount())
                            .build());
                }
            }

            return PropertyAnalyticsResponse.builder()
                    .marketInsights(calculateInsights(trends, transactionType,province, district, ward))
                    .trends(trends)
                    .build();

        } catch (Exception e) {
            log.error("Lỗi phân tích ES: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi hệ thống khi phân tích dữ liệu");
        }
    }

    @Override
    public List<WardPriceDTO> getPricesByWards(String province, String district, String propertyType, String transactionType) {
        try {
            // 1. Lọc bắt buộc: ACTIVE và đúng Quận (district)
            BoolQuery.Builder boolQuery = new BoolQuery.Builder()
                    .filter(f -> f.term(t -> t.field("status").value("ACTIVE")))
                    .filter(f -> f.term(t -> t.field("transactionType").value(transactionType)));
            if (province != null && !province.isEmpty()) {
                boolQuery.filter(f -> f.term(t -> t.field("province.keyword").value(province)));
            }
            if (district != null && !district.isEmpty()) {
                boolQuery.filter(f -> f.term(t -> t.field("district.keyword").value(district)));
            }
            if (propertyType != null && !propertyType.isEmpty()) {
                boolQuery.filter(f -> f.term(t -> t.field("propertyType").value(propertyType)));
            }

            String targetField = "FOR_SALE".equals(transactionType) ? "pricePerSqm" : "price";
            String unit = "FOR_SALE".equals(transactionType) ? "tr/m²" : "tr/tháng";

            // 2. Build Aggregation: Gom nhóm theo Phường (ward)
            SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index(indexName)
                    .size(0) // Không lấy bài đăng thô
                    .query(q -> q.bool(boolQuery.build()))
                    .aggregations("group_by_ward", a -> a
                            .terms(t -> t.field("ward.keyword").size(20)) // 🟢 Chỗ này sếp viết đúng chuẩn rồi
                            .aggregations("popular_price", sub -> sub.percentiles(p -> p.field(targetField).percents(50.0)))
                    )
            );

            SearchResponse<Void> response = esClient.search(searchRequest, Void.class);
            List<WardPriceDTO> result = new ArrayList<>();

            // 3. Bóc tách dữ liệu mảng Terms
            Aggregate aggregate = response.aggregations().get("group_by_ward");
            if (aggregate != null && aggregate.isSterms()) { // isSterms = String Terms
                for (co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket bucket : aggregate.sterms().buckets().array()) {

                    double medianVal = 0.0;
                    Aggregate popAgg = bucket.aggregations().get("popular_price");

                    if (popAgg != null && popAgg.isTdigestPercentiles()) {
                        String valStr = popAgg.tdigestPercentiles().values().keyed().get("50.0");
                        if (valStr != null && !valStr.equals("NaN")) {
                            medianVal = Double.parseDouble(valStr);
                        }
                    }

                    // Chia cho 1 triệu và làm tròn
                    long priceInMillion = BigDecimal.valueOf(medianVal)
                            .divide(BigDecimal.valueOf(1000000), RoundingMode.HALF_UP)
                            .longValue();

                    result.add(WardPriceDTO.builder()
                            .wardName(bucket.key().stringValue()) // Tên phường
                            .averagePrice(priceInMillion > 0 ? String.valueOf(priceInMillion) : "Đang cập nhật")
                            .unit(unit)
                            .totalPosts(bucket.docCount()) // Số lượng tin đăng
                            .build());
                }
            }

            return result;

        } catch (Exception e) {
            log.error("Lỗi khi thống kê giá theo phường: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi hệ thống khi thống kê dữ liệu");
        }
    }

    private MarketInsightDTO calculateInsights(List<PriceTrendDTO> trends, String transactionType, String province, String district, String ward) {
        if (trends == null || trends.isEmpty()) return null;

        PriceTrendDTO current = trends.get(trends.size() - 1);
        PriceTrendDTO peak = trends.stream().max((a, b) -> a.getAveragePrice().compareTo(b.getAveragePrice())).orElse(current);
        PriceTrendDTO lastYear = trends.size() >= 13 ? trends.get(trends.size() - 13) : trends.get(0);

        double currentVal = current.getAveragePrice().doubleValue();
        double lastYearVal = lastYear.getAveragePrice().doubleValue();
        double peakVal = peak.getAveragePrice().doubleValue();

        double growth = lastYearVal > 0 ? ((currentVal - lastYearVal) / lastYearVal) * 100 : 0;
        double diffPeak = peakVal > 0 ? Math.abs(((currentVal - peakVal) / peakVal) * 100) : 0;

        String unit = "FOR_SALE".equals(transactionType) ? "tr/m²" : "tr/tháng";
        long currentInMillion = current.getAveragePrice().divide(BigDecimal.valueOf(1000000), RoundingMode.HALF_UP).longValue();
        long peakInMillion = peak.getAveragePrice().divide(BigDecimal.valueOf(1000000), RoundingMode.HALF_UP).longValue();

        // 🟢 CẬP NHẬT: Ghép chuỗi địa điểm mượt mà (Phường, Quận, Tỉnh)
        StringBuilder locationBuilder = new StringBuilder();
        if (ward != null && !ward.trim().isEmpty()) locationBuilder.append(ward).append(", ");
        if (district != null && !district.trim().isEmpty()) locationBuilder.append(district).append(", ");
        if (province != null && !province.trim().isEmpty()) locationBuilder.append(province);

        String rawLocation = locationBuilder.toString().trim();
        if (rawLocation.endsWith(",")) rawLocation = rawLocation.substring(0, rawLocation.length() - 1);
        String locationText = rawLocation.isEmpty() ? " " : " tại " + rawLocation + " ";

        String yearlyGrowthLabel;
        if (trends.size() > 1) {
            yearlyGrowthLabel = "Giá " + (growth >= 0 ? "tăng" : "giảm") + locationText + "qua các tháng (" + lastYear.getMonth() + " - " + current.getMonth() + ")";
        } else {
            yearlyGrowthLabel = "Chưa đủ dữ liệu quá khứ để so sánh" + locationText.trim();
        }

        String diffFromPeakLabel;
        if (trends.size() > 1 && currentVal < peakVal) {
            diffFromPeakLabel = "Hiện tại thấp hơn đỉnh " + peakInMillion + " " + unit + " vào " + peak.getMonth();
        } else if (trends.size() > 1 && currentVal >= peakVal) {
            diffFromPeakLabel = "Giá hiện tại đang ở mức đỉnh lịch sử" + locationText.trim();
        } else {
            diffFromPeakLabel = "Mức giá hiện tại được ghi nhận là mốc khởi điểm";
        }

        return MarketInsightDTO.builder()
                .popularPriceText(String.valueOf(currentInMillion))
                .popularPriceUnit(unit)
                .popularPriceLabel("Giá phổ biến nhất" + locationText + "trong " + current.getMonth())
                .yearlyGrowthPercent(Math.round(Math.abs(growth) * 10.0) / 10.0)
                .yearlyGrowthTrend(growth >= 0 ? "UP" : "DOWN")
                .yearlyGrowthLabel(yearlyGrowthLabel)
                .diffFromPeakPercent(Math.round(diffPeak * 10.0) / 10.0)
                .diffFromPeakTrend(currentVal >= peakVal ? "UP" : "DOWN")
                .diffFromPeakLabel(diffFromPeakLabel)
                .build();
    }

    @Override
    public List<RegionTransactionStatDTO> getTopRegionsTransactionStats(int topK, String regionField) {
        try {
            // 1. Chỉ lấy các bài đăng đang ACTIVE
            BoolQuery.Builder boolQuery = new BoolQuery.Builder()
                    .filter(f -> f.term(t -> t.field("status").value("ACTIVE")));

            // Trường gom nhóm (Ví dụ: "province.keyword" hoặc "district.keyword")
            String groupByField = (regionField != null && !regionField.isEmpty()) ? regionField : "district.keyword";

            // 2. Build Aggregation Kép (Gom theo Tỉnh/Thành -> Sau đó gom theo Loại Giao Dịch)
            SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index(indexName)
                    .size(0) // Không lấy data thô, chỉ lấy thống kê
                    .query(q -> q.bool(boolQuery.build()))
                    .aggregations("group_by_region", a -> a
                            .terms(t -> t.field(groupByField).size(topK)) // Lấy Top K khu vực nhiều bài nhất
                            .aggregations("split_by_transaction", sub -> sub
                                    .terms(t2 -> t2.field("transactionType")) // Bóc tách Bán/Thuê
                            )
                    )
            );

            SearchResponse<Void> response = esClient.search(searchRequest, Void.class);
            List<RegionTransactionStatDTO> result = new ArrayList<>();

            // 3. Bóc tách dữ liệu JSON trả về từ ElasticSearch
            Aggregate regionAgg = response.aggregations().get("group_by_region");
            if (regionAgg != null && regionAgg.isSterms()) {
                for (co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket regionBucket : regionAgg.sterms().buckets().array()) {

                    String regionName = regionBucket.key().stringValue();
                    long totalPosts = regionBucket.docCount();
                    long saleCount = 0;
                    long rentCount = 0;

                    // Lấy cục Aggregation con (Bán/Thuê)
                    Aggregate transactionAgg = regionBucket.aggregations().get("split_by_transaction");
                    if (transactionAgg != null && transactionAgg.isSterms()) {
                        for (co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket transBucket : transactionAgg.sterms().buckets().array()) {
                            String type = transBucket.key().stringValue();
                            if ("FOR_SALE".equals(type)) {
                                saleCount = transBucket.docCount();
                            } else if ("FOR_RENT".equals(type)) {
                                rentCount = transBucket.docCount();
                            }
                        }
                    }

                    result.add(RegionTransactionStatDTO.builder()
                            .regionName(regionName)
                            .totalPosts(totalPosts)
                            .forSaleCount(saleCount)
                            .forRentCount(rentCount)
                            .build());
                }
            }

            return result;

        } catch (Exception e) {
            log.error("Lỗi khi thống kê loại giao dịch theo khu vực: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi hệ thống khi thống kê dữ liệu");
        }
    }
}