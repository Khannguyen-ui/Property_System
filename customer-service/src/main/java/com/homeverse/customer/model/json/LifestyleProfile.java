package com.homeverse.customer.model.json;
import lombok.Data;

@Data
public class LifestyleProfile {
    private String sleepTime;
    private boolean hasPet;
    private boolean smoking;
    private int cleanlinessLevel;
    private String personality;
}