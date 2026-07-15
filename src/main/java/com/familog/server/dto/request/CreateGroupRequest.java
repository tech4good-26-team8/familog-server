package com.familog.server.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateGroupRequest(
        @NotBlank(message = "그룹명을 입력해 주세요.")
        @Size(max = 50, message = "그룹명은 50자 이내로 입력해 주세요.")
        String name
) {}
