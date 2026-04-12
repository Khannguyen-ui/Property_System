package com.homeverse.property.controller;

import com.homeverse.property.dto.response.ProjectResponseDTO;
import com.homeverse.property.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/public/projects")
@RequiredArgsConstructor
public class PublicProjectController {

    private final ProjectService projectService;

    @GetMapping
    public ResponseEntity<Page<ProjectResponseDTO>> getAllProjects(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(projectService.getPublicProjects(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponseDTO> getDetail(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.getPublicProjectDetail(id));
    }
}