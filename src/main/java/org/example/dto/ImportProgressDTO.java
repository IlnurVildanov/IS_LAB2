package org.example.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportProgressDTO {
    private Long importId;
    private String fileName;
    private Integer currentProgress;
    private Integer totalRecords;
    private Integer processedRecords;
    private Integer successfulRecords;
    private Integer failedRecords;
    private String status;
    private String errorMessage;
}