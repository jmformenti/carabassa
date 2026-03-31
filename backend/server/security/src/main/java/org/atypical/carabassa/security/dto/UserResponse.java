package org.atypical.carabassa.security.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String username;
    private String role;
    private boolean enabled;
    private Instant createdAt;

}
