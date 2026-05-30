package com.homeverse.payment.service;

import com.homeverse.payment.dto.request.BillCreateDTO;
import com.homeverse.payment.dto.response.BillResponseDTO;

public interface BillService {
    BillResponseDTO createBill(BillCreateDTO dto);
}