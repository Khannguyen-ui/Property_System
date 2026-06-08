package com.homeverse.property.service.impl;

import com.homeverse.common.exception.AppException;
import com.homeverse.common.exception.ErrorCode;
import com.homeverse.property.dto.request.ProjectCreateDTO;
import com.homeverse.property.dto.response.ProjectResponseDTO;
import com.homeverse.property.entity.Project;
import com.homeverse.property.repository.ProjectRepository;
import com.homeverse.property.repository.PropertyRepository;
import com.homeverse.property.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final PropertyRepository propertyRepository;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectResponseDTO createProject(Long adminId, ProjectCreateDTO dto) {

        validateProjectData(dto);

        Point point = geometryFactory.createPoint(new Coordinate(dto.getLongitude(), dto.getLatitude()));

        Project project = Project.builder()
                .name(dto.getName().trim())
                .description(dto.getDescription())
                .imageUrl(cleanImageUrl(dto.getImageUrl()))
                .address(dto.getAddress().trim())
                .location(point)

                .projectType(Project.ProjectType.valueOf(dto.getProjectType().trim().toUpperCase()))
                .amenities(dto.getAmenities())
                .createdBy(adminId)
                .status(Project.Status.ACTIVE)
                .build();

        Project savedProject = projectRepository.save(project);
        return mapToResponse(savedProject);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectResponseDTO updateProject(Long adminId, Long id, ProjectCreateDTO dto) {

        validateProjectData(dto);

        Point point = geometryFactory.createPoint(new Coordinate(dto.getLongitude(), dto.getLatitude()));

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST));

        project.setName(dto.getName().trim());
        project.setDescription(dto.getDescription());
        project.setImageUrl(cleanImageUrl(dto.getImageUrl()));
        project.setAddress(dto.getAddress().trim());

        project.setProjectType(Project.ProjectType.valueOf(dto.getProjectType().trim().toUpperCase()));
        project.setAmenities(dto.getAmenities());
        project.setLocation(point);

        Project updatedProject = projectRepository.save(project);
        return mapToResponse(updatedProject);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProject(Long adminId, Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST));

        projectRepository.delete(project);
    }


    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<ProjectResponseDTO> getDeletedProjects(int page, int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        return projectRepository.findAllDeletedProjects(pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restoreProject(Long adminId, Long id) {
        // Kiểm tra xem có trong thùng rác không
        if (!projectRepository.existsDeletedById(id)) {
            throw new AppException(ErrorCode.INVALID_REQUEST); // hoặc INVALID_REQUEST
        }

        // Thực hiện restore
        projectRepository.restoreById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void hardDeleteProject(Long adminId, Long id) {
        if (!projectRepository.existsDeletedById(id)) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        propertyRepository.detachPropertiesFromProject(id);
        projectRepository.hardDeleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<ProjectResponseDTO> getAllProjectsForAdmin(int page, int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);

        // Admin được xem toàn bộ (cả ACTIVE và INACTIVE), chỉ giấu rác (DELETED) đi thôi
        return projectRepository.findByStatusNot(Project.Status.DELETED, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectResponseDTO getProjectDetailForAdmin(Long id) {
        // Móc dự án lên cho Admin xem chi tiết (không cho xem dự án đã vô thùng rác)
        Project project = projectRepository.findByIdAndStatusNot(id, Project.Status.DELETED)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST)); // Hoặc PROJECT_NOT_FOUND

        return mapToResponse(project);
    }

    // Lấy danh sách Public (Sếp dùng hàm findByStatus đã có)
    @Transactional(readOnly = true)
    public Page<ProjectResponseDTO> getPublicProjects(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return projectRepository.findByStatus(Project.Status.ACTIVE, pageable)
                .map(this::mapToResponse);
    }

    // Lấy chi tiết Public (Dùng hàm mới thêm ở Bước 1)
    @Transactional(readOnly = true)
    public ProjectResponseDTO getPublicProjectDetail(Long id) {
        Project project = projectRepository.findByIdAndStatus(id, Project.Status.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST)); // Nên có mã lỗi PROJECT_NOT_FOUND
        return mapToResponse(project);
    }

    private void validateProjectData(ProjectCreateDTO dto) {
        if (dto.getLatitude() == null || dto.getLatitude() < -90 || dto.getLatitude() > 90 ||
                dto.getLongitude() == null || dto.getLongitude() < -180 || dto.getLongitude() > 180) {
            throw new AppException(ErrorCode.LOCATION_REQUIRED);
        }

        try {
            // SỬA ĐỔI 3: Bọc lỗi cực mạnh ở đây
            String safeProjectType = dto.getProjectType().trim().toUpperCase();
            Project.ProjectType.valueOf(safeProjectType);
        } catch (Exception e) {
            // ĐỂ LỘ DIỆN KẺ THÙ: In ra xem Postman gửi lên chữ gì mà lỗi
            System.out.println(">>> LỖI ENUM! Chữ gửi lên từ Postman là: [" + dto.getProjectType() + "]");
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
    }

    private ProjectResponseDTO mapToResponse(Project project) {
        ProjectResponseDTO dto = new ProjectResponseDTO();
        dto.setId(project.getId());
        dto.setName(project.getName());
        dto.setDescription(project.getDescription());
        dto.setImageUrl(project.getImageUrl());
        dto.setAddress(project.getAddress());

        if (project.getLocation() != null) {
            dto.setLongitude(project.getLocation().getX());
            dto.setLatitude(project.getLocation().getY());
        }

        if (project.getProjectType() != null) {
            dto.setProjectType(project.getProjectType().name());
        }

        if (project.getStatus() != null) {
            dto.setStatus(project.getStatus().name());
        }

        dto.setAmenities(project.getAmenities());
        dto.setCreatedAt(project.getCreatedAt());

        return dto;
    }
    private String cleanImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }
        return imageUrl.trim();
    }
}