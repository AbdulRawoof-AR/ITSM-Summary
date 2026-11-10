package com.example.itsm_summary.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "support_tickets")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ticket {
    @Id
    private String id;
    private String title;
    private String description;
    private String category;
    private String subCategory;
    private String status;
    private String priority;
    private String agent;
    private String agentId;
    private String crmId;
    private String subscriptionId;
    private String StudioLoginId;
    private String eMail;
    private String createdBy;
    private String lastModifiedBy;
    private String closedBy;
    private String closureNotes;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalDateTime createdDate;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalDateTime updatedAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalDateTime lastModifiedDate;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalDateTime closedAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalDateTime closedDate;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalDateTime dueAt;

    public String getTicketId() {
        return id;
    }

    public void setTicketId(String ticketId) {
        this.id = ticketId;
    }
}
