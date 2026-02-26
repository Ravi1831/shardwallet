package com.ravi.mds.shardedsagawallet.service.saga;

import com.ravi.mds.shardedsagawallet.entity.SagaInstance;
import com.ravi.mds.shardedsagawallet.entity.SagaStatus;
import com.ravi.mds.shardedsagawallet.entity.SagaStep;
import com.ravi.mds.shardedsagawallet.entity.StepStatus;
import com.ravi.mds.shardedsagawallet.repository.SagaInstanceRepository;
import com.ravi.mds.shardedsagawallet.repository.SagaStepRepository;
import com.ravi.mds.shardedsagawallet.service.saga.steps.SagaStepFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SagaOrchestratorImpl implements SagaOrchestrator {

    private final ObjectMapper objectMapper;
    private final SagaInstanceRepository sagaInstanceRepository;
    private final SagaStepFactory sagaStepFactory;
    private final SagaStepRepository sagaStepRepository;

    @Override
    @Transactional
    public Long startSaga(SagaContext context) {
        try {
            String contextJson = objectMapper.writeValueAsString(context);
            SagaInstance sagaInstance = SagaInstance.builder()
                    .context(contextJson)
                    .status(SagaStatus.STARTED)
                    .build();
            SagaInstance savedSagaInstance = sagaInstanceRepository.save(sagaInstance);
            log.info("Started saga with id {}", savedSagaInstance.getId());
            return savedSagaInstance.getId();
        } catch (JacksonException e) {
            log.info("Error Starting Saga", e);
            throw new RuntimeException("Error starting saga", e.getCause());
        }
    }

    @Override
    @Transactional
    public boolean executeStep(Long sagaInstanceId, String stepName) {
        SagaInstance sagaInstance = sagaInstanceRepository.findById(sagaInstanceId)
                .orElseThrow(() -> new RuntimeException("Saga instance not found"));

        SagaStepInterface step = sagaStepFactory.getSagaStep(stepName);

        SagaStep sagaStepDB = sagaStepRepository
                .findBySagaInstanceIdAndStepNameAndStatus(sagaInstanceId, stepName, StepStatus.PENDING)
                .orElse(SagaStep.builder()
                        .sagaInstanceId(sagaInstanceId)
                        .stepName(stepName)
                        .status(StepStatus.PENDING)
                        .build());


        if (sagaStepDB.getId() == null) {
            sagaStepRepository.save(sagaStepDB);
        }

        try {
            SagaContext sagaContext = objectMapper.readValue(sagaInstance.getContext(), SagaContext.class);
            sagaStepDB.markAsRunning();
            sagaStepRepository.save(sagaStepDB); //updating the status to running in db
            boolean success = step.execute(sagaContext);
            if (success) {
                sagaStepDB.markAsCompleted();
                sagaStepRepository.save(sagaStepDB); //updating the status to completed in db

                sagaInstance.setCurrentStep(stepName); //step we just completed
                sagaInstance.markAsRunning();
                sagaInstanceRepository.save(sagaInstance); //updating the status to running in db

                log.info("Step {} executed successfully.", stepName);
                return true;
            } else {
                sagaStepDB.markAsFailed();
                sagaStepRepository.save(sagaStepDB);
                log.error("Step {} failed. ", stepName);
                return false;
            }

        } catch (JacksonException e) {
            sagaStepDB.markAsFailed();
            sagaStepRepository.save(sagaStepDB);
            log.error("Failed to execute step {}", stepName);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean compensateStep(Long sagaInstanceId, String stepName) {
        // * 1. Fetch the saga instance from the db using saga instance id
        SagaInstance sagaInstance = sagaInstanceRepository.findById(sagaInstanceId)
                .orElseThrow(() -> new RuntimeException("Saga instance not found"));
        // * 2. Fetch the saga step from db using the saga instance id and step name
        SagaStepInterface step = sagaStepFactory.getSagaStep(stepName);
        if (step == null){
            log.error("saga step not found with the name {}",stepName);
            throw new RuntimeException("Saga step not found");
        }
        // * 3. Take the context form the saga instance and call the compensation method of the step
        SagaStep sagaStepDB = sagaStepRepository
                .findBySagaInstanceIdAndStepNameAndStatus(sagaInstanceId, stepName, StepStatus.COMPLETED)
                .orElse(null);
        if (sagaStepDB == null){
            log.warn("Step {} not found in the db for the saga instance {}, so it is already compensated or not executed.",stepName,sagaInstance);
            return true;
        }
        // * 4. update the appropriate status in the saga step
        try{
            SagaContext sagaContext = objectMapper.readValue(sagaInstance.getContext(), SagaContext.class);
            sagaStepDB.markAsCompensating();
            sagaStepRepository.save(sagaStepDB);

            boolean success = step.compensate(sagaContext);

            if (success){
                sagaStepDB.markAsCompensated();
                sagaStepRepository.save(sagaStepDB);
                log.info("Step {} compensated successfully ",stepName);
                return true;
            }else {
                sagaStepDB.markAsFailed();
                sagaStepRepository.save(sagaStepDB);
                log.error("Step {} failed. ", stepName);
                return false;
            }
        }catch (Exception e){
            sagaStepDB.markAsFailed();
            sagaStepRepository.save(sagaStepDB);
            log.error("Failed to execute step {}", stepName);
            return false;
        }
    }

    @Override
    public SagaInstance getSagaInstance(Long sagaInstanceId) {
        return sagaInstanceRepository.findById(sagaInstanceId).orElseThrow(() -> new RuntimeException("No saga instance found with the id " + sagaInstanceId));
    }

    @Override
    @Transactional
    public void compensateSaga(Long sagaInstanceId) {
        SagaInstance sagaInstance = sagaInstanceRepository.findById(sagaInstanceId).orElseThrow(() -> new RuntimeException("No saga instance found with the id " + sagaInstanceId));
        //mark the saga as compensating in db
        sagaInstance.markAsCompensating();
        sagaInstanceRepository.save(sagaInstance);
        //get all the completed steps
        List<SagaStep> completedSteps = sagaStepRepository.findCompletedBySagaInstanceId(sagaInstanceId);

        boolean allCompensated = true;
        for (SagaStep completedStep : completedSteps) {
            boolean completed = this.compensateStep(sagaInstanceId, completedStep.getStepName());
            if (!completed) {
                allCompensated = false;
            }
        }
        if (allCompensated) {
            sagaInstance.markAsCompensated();
            sagaInstanceRepository.save(sagaInstance);
            log.info("Saga {} compensated successfully", sagaInstanceId);
        } else {
            log.error("Saga {} compensation failed ", sagaInstanceId);
        }
    }

    @Override
    @Transactional
    public void failSaga(Long sagaInstanceId) {
        SagaInstance sagaInstance = sagaInstanceRepository.findById(sagaInstanceId).orElseThrow(() -> new RuntimeException("No saga instance found with the id " + sagaInstanceId));
        sagaInstance.markAsFailed();
        sagaInstanceRepository.save(sagaInstance);
    }

    @Override
    @Transactional
    public void completeSaga(Long sagaInstanceId) {
        SagaInstance sagaInstance = sagaInstanceRepository.findById(sagaInstanceId).orElseThrow(() -> new RuntimeException("No saga instance found with the id " + sagaInstanceId));
        sagaInstance.markAsCompleted();
        sagaInstanceRepository.save(sagaInstance);

    }
}
