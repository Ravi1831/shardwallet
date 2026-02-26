package com.ravi.mds.shardedsagawallet.service.saga.steps;

import com.ravi.mds.shardedsagawallet.service.saga.SagaStepInterface;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class SagaStepFactory {

    private final Map<String, SagaStepInterface> stepByName;

    public SagaStepFactory(List<SagaStepInterface> sagaStep) {
        this.stepByName = sagaStep.stream()
                .collect(Collectors.toMap(SagaStepInterface::getStepName, Function.identity()));
        System.err.println("printing map for sagaStepFactory: "+stepByName);
    }

    public SagaStepInterface getSagaStep(String stepName){
        SagaStepInterface sagaStep = stepByName.get(stepName);
        if (sagaStep == null){
            log.error("Saga step not found for the step name {}",stepName);
            throw new IllegalArgumentException("Unknown saga step: " + stepName);
        }
        return sagaStep;
    }
}
