package com.homeverse.property.service;

import com.homeverse.property.dto.request.ProjectCreateDTO;
import com.homeverse.property.dto.response.ProjectResponseDTO;
import org.springframework.data.domain.Page;

public interface ProjectService {
    // Đã bổ sung thêm tham số "" vào cả 3 hàm
    ProjectResponseDTO createProject(Long adminId, ProjectCreateDTO dto);

    ProjectResponseDTO updateProject(Long adminId, Long id, ProjectCreateDTO dto);

    void deleteProject(Long adminId, Long id);


    void hardDeleteProject(Long adminId, Long id);

    void restoreProject(Long adminId, Long id);

    org.springframework.data.domain.Page<ProjectResponseDTO> getDeletedProjects(int page, int size);

    org.springframework.data.domain.Page<ProjectResponseDTO> getAllProjectsForAdmin(int page, int size);

    ProjectResponseDTO getProjectDetailForAdmin(Long id);

    Page<ProjectResponseDTO> getPublicProjects(int page, int size);

    ProjectResponseDTO getPublicProjectDetail(Long id);
}