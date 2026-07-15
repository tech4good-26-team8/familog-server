package com.familog.server.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.familog.server.dto.request.CreateGroupRequest;
import com.familog.server.dto.response.GroupResponse;
import com.familog.server.service.GroupService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "group", description = "가족 그룹 / 초대코드")
@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @Operation(summary = "그룹 생성 (방 만들기)", description = "그룹명으로 생성하고 6자리 고유 초대코드를 자동 발급한다. 방장은 발급된 코드로 가입 API를 그대로 탄다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GroupResponse create(@Valid @RequestBody CreateGroupRequest request) {
        return groupService.create(request);
    }

    @Operation(summary = "그룹 조회", description = "그룹명·초대코드 조회. '가족 코드 복사'·'초대 링크 복사' 공용 (링크는 프론트가 코드로 딥링크 조립).")
    @GetMapping("/{groupId}")
    public GroupResponse get(@PathVariable Long groupId) {
        return groupService.get(groupId);
    }
}
