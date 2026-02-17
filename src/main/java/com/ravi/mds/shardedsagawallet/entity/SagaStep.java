package com.ravi.mds.shardedsagawallet.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.SQLType;

@Getter
@Setter
@SuperBuilder
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "saga_step")
public class SagaStep extends BaseEntity {

    @Column(name = "saga_instance_id", nullable = false)
    private Long sagaInstanceId;
    @Column(name = "step_name", nullable = false)
    private String stepName;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StepStatus status;
    @Column(name = "error_message", nullable = true)
    private String errorMessage;

    //json step data
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "step_data",columnDefinition = "json")
    private String stepData;
}
