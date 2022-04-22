package com.devu.backend.controller.comment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class CommentResponseDto {
    private String username;
    private String title;
    private Long commentId;
}
