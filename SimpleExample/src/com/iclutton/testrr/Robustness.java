package com.iclutton.testrr;

import com.ragstorooks.testrr.Runner;
import com.ragstorooks.testrr.ScenarioBase;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import static junit.framework.Assert.assertEquals;

public class Robustness {

    @Test
    public void robustnesRun(){
        TestOne t1 = new TestOne();
        TestTwo t2 = new TestTwo();

        Map<ScenarioBase, Integer> weightings = new HashMap<ScenarioBase, Integer>();
        weightings.put(t1, 2);
        weightings.put(t2, 1);

        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(4);

        Runner runner = new Runner();
        runner.setNumberOfConcurrentStarts(5);
        runner.setNumberOfRuns(100);
        runner.setScenarioWeightings(weightings);
        runner.setScheduledExecutorService(executor);

        runner.run();

        assertEquals(100.0, runner.getSuccessRate());

    }

}
