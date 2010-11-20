package com.iclutton.testrr;

import com.ragstorooks.testrr.ScenarioBase;

public class TestOne extends ScenarioBase {

    @Override
    public void run(String scenarioId) {

        // do stuff

        getScenarioListener().scenarioFailure(scenarioId, "whoops");
    
    }
}
