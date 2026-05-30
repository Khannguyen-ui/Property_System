package com.homeverse.common.kafka.cdc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DebeziumMessage<T> {
    private T before;
    private T after;
    private String op;
}