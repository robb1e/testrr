package com.iclutton.testrr;

import com.ragstorooks.testrr.ScenarioBase;

public class TestTwo extends ScenarioBase {

    @Override
    public void run(String scenarioId) {

        // do stuff

        getScenarioListener().scenarioSuccess(scenarioId);

    }
}
