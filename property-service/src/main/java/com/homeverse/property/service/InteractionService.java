package com.homeverse.property.service;

public interface InteractionService {

    boolean toggleLike(Long userId,String guestId, Long propertyId);

    boolean toggleSave(Long userId,String guestId, Long propertyId);
}