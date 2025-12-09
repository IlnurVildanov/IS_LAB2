package org.example.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Entity
@Table(name = "import_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ImportHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_type", nullable = false)
    private String fileType;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ImportStatus status;

    @Column(name = "user_name", nullable = false)
    private String userName;

    @Column(name = "is_admin", nullable = false)
    private Boolean isAdmin;

    @Column(name = "start_time", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date startTime;

    @Column(name = "end_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date endTime;

    @Column(name = "total_records")
    private Integer totalRecords;

    @Column(name = "successful_records")
    private Integer successfulRecords;

    @Column(name = "failed_records")
    private Integer failedRecords;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "current_progress")
    private Integer currentProgress;

    public enum ImportStatus {
        IN_PROGRESS,
        COMPLETED,
        FAILED
    }
}