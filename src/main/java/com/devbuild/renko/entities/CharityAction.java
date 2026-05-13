package com.devbuild.renko.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Document(collection = "charity_actions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CharityAction {
    @Id
    private String id;

    private Long organizationId;
    
    private String title;
    private String category;
    private String description;
    private LocalDate date;
    private String location;
    
    private Double targetAmount;
    private Double currentAmount;

    private List<String> mediaUrls;
    private String mainImageUrl;
    
    private boolean isArchived;
}
