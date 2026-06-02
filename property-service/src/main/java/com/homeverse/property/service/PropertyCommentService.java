package com.homeverse.property.service;

import com.homeverse.property.dto.request.CommentRequest;
import com.homeverse.property.dto.response.CommentResponse;
import org.springframework.data.domain.Page;

public interface PropertyCommentService {

    CommentResponse createComment(Long userId, String guestId, CommentRequest request);

    Page<CommentResponse> getComments(Long propertyId, int page, int size);

    Page<CommentResponse> getReplies(Long parentId, int page, int size);

    void deleteComment(Long commentId, Long userId);

    long countComments(Long propertyId);
}