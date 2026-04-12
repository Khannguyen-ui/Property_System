    package com.homeverse.property.repository;

    import com.homeverse.property.entity.Project;
    import org.springframework.data.domain.Page;
    import org.springframework.data.domain.Pageable;
    import org.springframework.data.jpa.repository.JpaRepository;
    import org.springframework.data.jpa.repository.Modifying;
    import org.springframework.data.jpa.repository.Query;
    import org.springframework.data.repository.query.Param;
    import org.springframework.stereotype.Repository;

    import java.util.Optional;

    @Repository
    public interface ProjectRepository extends JpaRepository<Project, Long> {

        // ==========================================
        // ---> 1. CÁC HÀM "BẢO VỆ TAY" (THAY THẾ CHỨC NĂNG CỦA BÙA CŨ) <---
        // ==========================================

        // Thay thế cho findById() -> Chỉ tìm những dự án CHƯA BỊ XÓA
        Optional<Project> findByIdAndStatusNot(Long id, Project.Status status);

        // Thay thế cho findAll() -> Lấy danh sách cho Admin (Cả ACTIVE và INACTIVE, trừ DELETED ra)
        Page<Project> findByStatusNot(Project.Status status, Pageable pageable);

        // Lấy danh sách cho Khách hàng vãng lai (Chỉ lấy dự án ACTIVE)
        Page<Project> findByStatus(Project.Status status, Pageable pageable);
        // Detail dự án
        Optional<Project> findByIdAndStatus(Long id, Project.Status status);


        // ==========================================
        // ---> 2. API THÙNG RÁC (QUAY VỀ JPQL CHO AN TOÀN, MƯỢT MÀ) <---
        // ==========================================
        @Query(value = "SELECT COUNT(*) > 0 FROM projects WHERE id = :id AND status = 'DELETED'",
                nativeQuery = true)
        boolean existsDeletedById(@Param("id") Long id);


        @Query(value = "SELECT * FROM projects WHERE status = 'DELETED'",
                countQuery = "SELECT COUNT(*) FROM projects WHERE status = 'DELETED'",
                nativeQuery = true)
        Page<Project> findAllDeletedProjects(Pageable pageable);


        // ==========================================
        // ---> 3. UPDATE/DELETE BẰNG NATIVE QUERY (GIỮ NGUYÊN) <---
        // ==========================================

        @Modifying(clearAutomatically = true, flushAutomatically = true)
        @Query(value = "UPDATE projects SET status = 'ACTIVE' WHERE id = :id", nativeQuery = true)
        void restoreById(@Param("id") Long id);


        @Modifying(clearAutomatically = true, flushAutomatically = true)
        @Query(value = "DELETE FROM projects WHERE id = :id", nativeQuery = true)
        void hardDeleteById(@Param("id") Long id);
    }