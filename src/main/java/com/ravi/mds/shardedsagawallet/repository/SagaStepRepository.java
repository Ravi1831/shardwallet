package com.ravi.mds.shardedsagawallet.repository;

import com.ravi.mds.shardedsagawallet.entity.SagaStep;
import com.ravi.mds.shardedsagawallet.entity.StepStatus;
import com.ravi.mds.shardedsagawallet.service.saga.SagaStepInterface;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SagaStepRepository extends JpaRepository<SagaStep, Long> {

    List<SagaStep> findBySagaInstanceId(Long sagaInstanceId);

    List<SagaStep> findBySagaInstanceIdAndStatus(Long sagaInstanceId, StepStatus status);

    @Query("""
            SELECT s
            FROM SagaStep s
            WHERE s.sagaInstanceId = :sagaInstanceId
            AND s.status = 'COMPLETED'
            """)
    List<SagaStep> findCompletedStepBySagaInstanceId(@Param("sagaInstanceId") Long sagaInstanceId);


    @Query("""
            SELECT s
            FROM SagaStep s
            WHERE s.sagaInstanceId = :sagaInstanceId
            AND s.status = com.ravi.mds.shardedsagawallet.entity.StepStatus.COMPLETED
            """)
    List<SagaStep> findCompletedBySagaInstanceId(@Param("sagaInstanceId") Long sagaInstanceId);

    Optional<SagaStep> findBySagaInstanceIdAndStepName(Long sagaInstanceId, String stepName);
    Optional<SagaStep> findBySagaInstanceIdAndStepNameAndStatus(Long sagaInstanceId, String stepName,StepStatus status);

}