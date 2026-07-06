package com.jitong.projectflow.auth.dto;

public record LoginResponse(String token, Long userId, String realName) {
}
