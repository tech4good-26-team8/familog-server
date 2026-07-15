package com.familog.server.dto.request;

import jakarta.validation.constraints.NotNull;

/** senderId가 있으면 그 멤버 메시지만(확대 뷰), 없으면 그룹 전체(채팅방 진입) 읽음 처리 */
public record ReadMessagesRequest(
        @NotNull(message = "읽는 사람은 필수입니다.")
        Long readerId,

        Long senderId
) {}
