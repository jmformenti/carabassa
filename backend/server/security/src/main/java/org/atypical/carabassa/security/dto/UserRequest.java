package org.atypical.carabassa.security.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserRequest {

    @NotBlank
    private String username;

    private String password;  // nullable on update (no change)

    @NotBlank
    private String role;

    private boolean enabled = true;

    private String defaultDataset;

}
