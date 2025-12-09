package org.example.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.entity.ImportHistory;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportHistoryDTO {
    private Long id;
    private String fileName;
    private String fileType;
    private ImportHistory.ImportStatus status;
    private String userName;
    private Boolean isAdmin;
    private Date startTime;
    private Date endTime;
    private Integer totalRecords;
    private Integer successfulRecords;
    private Integer failedRecords;
    private String errorMessage;
    private Integer currentProgress;
}