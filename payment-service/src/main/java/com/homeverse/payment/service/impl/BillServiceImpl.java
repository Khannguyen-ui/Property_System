package com.homeverse.payment.service.impl;

import com.homeverse.payment.dto.request.BillCreateDTO;
import com.homeverse.payment.dto.response.BillResponseDTO;
import com.homeverse.payment.entity.Bill;
import com.homeverse.payment.repository.BillRepository;
import com.homeverse.payment.service.BillService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class BillServiceImpl implements BillService {

    private final BillRepository billRepository;

    @Override
    @Transactional
    public BillResponseDTO createBill(BillCreateDTO dto) {
        // Tìm hóa đơn gần nhất dựa trên ID hợp đồng
        Bill lastBill = billRepository.findTopByContractIdOrderByCreatedAtDesc(dto.getContractId())
                .orElse(null);

        int electricOld = (lastBill != null) ? lastBill.getElectricNew() : 0;
        int waterOld = (lastBill != null) ? lastBill.getWaterNew() : 0;

        if (dto.getElectricNew() < electricOld || dto.getWaterNew() < waterOld) {
            throw new RuntimeException("Chỉ số mới không được nhỏ hơn chỉ số cũ!");
        }

        // Tính toán số tiền dựa trên thông tin từ DTO (Vì Payment không giữ bảng Contract)
        BigDecimal electricCost = dto.getElectricPrice()
                .multiply(BigDecimal.valueOf(dto.getElectricNew() - electricOld));

        BigDecimal waterCost = dto.getWaterPrice()
                .multiply(BigDecimal.valueOf(dto.getWaterNew() - waterOld));

        // Tổng tiền = Tiền thuê + Điện + Nước + Phí dịch vụ
        BigDecimal totalAmount = dto.getMonthlyRent()
                .add(electricCost)
                .add(waterCost)
                .add(dto.getServiceFees());

        Bill bill = Bill.builder()
                .contractId(dto.getContractId())
                .month(dto.getMonth())
                .year(dto.getYear())
                .electricOld(electricOld)
                .waterOld(waterOld)
                .electricNew(dto.getElectricNew())
                .waterNew(dto.getWaterNew())
                .totalAmount(totalAmount)
                .status(Bill.Status.UNPAID)
                .build();

        Bill savedBill = billRepository.save(bill);

        // Lưu ý: Phần Notification nên bắn qua Kafka thay vì gọi Service trực tiếp để tránh lỗi compile
        
        return BillResponseDTO.builder()
                .id(savedBill.getId())
                .totalAmount(savedBill.getTotalAmount())
                .status(savedBill.getStatus().toString())
                .build();
    }
}