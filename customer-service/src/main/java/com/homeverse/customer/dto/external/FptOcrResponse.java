package com.homeverse.customer.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty; // Sếp nhớ import cái này vào để nó map được tên JSON
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FptOcrResponse {
    private int errorCode;
    private String errorMessage;
    private List<DataDetail> data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DataDetail {


        private String id;
        private String name;
        private String dob;
        private String address;


        @JsonProperty("id_prob")
        private String idProb;

        @JsonProperty("name_prob")
        private String nameProb;

        @JsonProperty("address_prob")
        private String addressProb;
    }
}