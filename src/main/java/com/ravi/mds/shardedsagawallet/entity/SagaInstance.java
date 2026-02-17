package com.ravi.mds.shardedsagawallet.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@SuperBuilder
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "saga_instance")
public class SagaInstance extends BaseEntity{

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status",nullable = false)
    private SagaStatus status = SagaStatus.STARTED;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "context",columnDefinition = "json")
    private String context;

    @Column(name = "current_step",nullable = false)
    private String currentStep;
}
