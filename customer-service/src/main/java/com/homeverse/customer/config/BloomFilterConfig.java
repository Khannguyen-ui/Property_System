package com.homeverse.customer.config;

import com.homeverse.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;

import java.util.concurrent.CompletableFuture;

@Configuration
@RequiredArgsConstructor
public class BloomFilterConfig {

    private final CustomerRepository customerRepository;

    @Bean
    public RBloomFilter<String> citizenIdBloomFilter(RedissonClient redissonClient) {
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter("citizenIdFilter");

        bloomFilter.tryInit(5_000_000L, 0.01);
        return bloomFilter;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initBloomFilterData(ApplicationReadyEvent event) {
        RBloomFilter<String> bloomFilter = event.getApplicationContext().getBean("citizenIdBloomFilter", RBloomFilter.class);


        long dbKycCount = customerRepository.countByCitizenIdIsNotNull();


        if (bloomFilter.count() < dbKycCount) {
            System.out.println(" Bắt đầu đồng bộ dữ liệu CCCD lên Bloom Filter (Chạy ngầm)...");


            CompletableFuture.runAsync(() -> {
                int batchSize = 10000;
                int pageNumber = 0;
                Slice<String> slice;

                try {
                    do {

                        slice = customerRepository.findCitizenIdsInBatches(PageRequest.of(pageNumber, batchSize));


                        for (String id : slice.getContent()) {
                            bloomFilter.add(id);
                        }

                        System.out.println("   -> Đã đồng bộ batch " + pageNumber + " (" + slice.getContent().size() + " records)");
                        pageNumber++;

                    } while (slice.hasNext());

                    System.out.println("Đồng bộ Bloom Filter HOÀN TẤT! Sẵn sàng phục vụ hàng triệu Users.");

                } catch (Exception e) {
                    System.err.println(" Lỗi khi đồng bộ Bloom Filter: " + e.getMessage());
                }
            });
        }
    }
}