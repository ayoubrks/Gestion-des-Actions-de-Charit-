package com.devbuild.renko.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.*;
import java.time.LocalDateTime;

@Document(collection = "donations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Donation {
    @Id
    private String id;

    private Long userId;
    private String charityActionId;
    private Double amount;
    private LocalDateTime date;
    private String paymentMethod;
    private String status;
}
