package org.geogig.server.websockets.internal;

import java.time.Instant;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Lob;
import javax.persistence.Table;

import org.geogig.web.model.ServerEvent;
import org.geogig.web.model.ServerEvent.EventTypeEnum;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//@formatter:off
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table( name = "activity_log", 
        indexes = {
            @Index(columnList = "timestamp DESC",  unique=false, name="activity_log_timestamp_idx"),
            @Index(columnList = "event",           unique=false, name="activity_log_event_idx"),
            @Index(columnList = "event_type",      unique=false, name="activity_log_event_type_idx"),
            @Index(columnList = "event_issuer",    unique=false, name="activity_log_issuer_idx"),
            @Index(columnList = "store",           unique=false, name="activity_log_store_idx"),
            @Index(columnList = "repo_owner",      unique=false, name="activity_log_repo_owner_idx"),
            @Index(columnList = "repo",            unique=false, name="activity_log_repo_idx"),
            @Index(columnList = "branch",          unique=false, name="activity_log_branch_idx"),
            @Index(columnList = "layer",           unique=false, name="activity_log_layer_idx")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityEntry {//@formatter:on

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    // not using @CreatedDate here, it's set to the event timestamp, not the time the record is
    // added to the database. The difference could be milliseconds, but still.
    @Column(name = "timestamp", updatable = false, nullable = false)
    private Instant timestamp;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    private @Column(name = "event", nullable = false) String event;

    @Enumerated(EnumType.ORDINAL)
    private @Column(name = "event_type", nullable = true) EventTypeEnum eventType;

    private @Column(name = "event_issuer") UUID issuer;

    private @Column(name = "store") UUID store;

    private @Column(name = "repo_owner") UUID repoOwner;

    private @Column(name = "repo") UUID repo;

    private @Column(name = "branch") String branch;

    private @Column(name = "layer") String layer;

    @Lob
    @Column(name = "payload_json")
    @Convert(converter = JsonJPAConverter.class)
    private ServerEvent payload;

}
