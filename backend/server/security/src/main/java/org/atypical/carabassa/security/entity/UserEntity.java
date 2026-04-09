package org.atypical.carabassa.security.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "USERS")
@SequenceGenerator(name = "user_id_gen", sequenceName = "user_sequence", allocationSize = 1)
@Getter
@Setter
@NoArgsConstructor
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "user_id_gen")
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String role;

    @Column
    private String defaultDataset;

    @Column(nullable = false)
    private boolean enabled = true;
 
    @Column(nullable = false)
    private int failedLoginAttempts = 0;

    @Column(nullable = false)
    private Instant createdAt;

    public UserEntity(String username, String passwordHash, String role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    @PrePersist
    public void onPrePersist() {
        this.createdAt = Instant.now();
    }

}
