package com.devbuild.renko.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "organizations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Organization {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String legalAddress;

    @Column(nullable = false, unique = true)
    private String taxId;

    @Column(nullable = false)
    private String mainContact;

    private String logoUrl;

    @Column(columnDefinition = "TEXT")
    private String description;

    private boolean approved;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User adminUser;
}
