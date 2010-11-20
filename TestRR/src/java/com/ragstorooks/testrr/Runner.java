/*
Copyright 2008 Raghav Ramesh

This file is part of TestRR.

TestRR is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

TestRR is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with TestRR.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.ragstorooks.testrr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Runner is the main class which does all the work for TestRR. Scenarios are executed concurrently to simulate a load
 * on the system under test. The number of times a scenario is executed depends on its weighting. The preferred mode of
 * execution can be configured explicitly using the exposed parameters.
 */
public class Runner implements ScenarioListener {
    private static final Log log = LogFactory.getLog(Runner.class);
    private static final int HUNDRED = 100;
    private static final Random randomGenerator = new Random();

    private int numberOfRuns = 500;
    private int numberOfConcurrentStarts = 5;
    private long coolDownPeriod = 1000;
    private boolean synchronizedScheduling = false;
    private int synchronizeWaitMilliSeconds = 1000;
    private long totalRunTimeMilliSeconds = 0;
    private boolean useDeterministicNumberOfRuns = false;

    private Map<ScenarioBase, Integer> scenarioWeightings = null;
    private ScheduledExecutorService scheduledExecutorService = null;

    private int scenariosCounter = 0;
    private ArrayList<ScenarioBase> scenariosList;
    private ArrayList<ScenarioBase> weightedScenariosList;
    private Map<String, ScenarioResult> scenarioSuccesses = new HashMap<String, ScenarioResult>();
    private Map<String, ScenarioResult> scenarioFailures = new HashMap<String, ScenarioResult>();

    private CountDownLatch scenarioStartLatch;
    private CountDownLatch scenarioCompleteLatch;

    /**
     * To be invoked when all the parameters have been configured, to generate the load on the system under test.
     */
    public void run() {
        log.info(String.format("RPRunner(numberOfRuns=%d, numberOfConcurrentStarts=%d)", numberOfRuns, numberOfConcurrentStarts));

        if (useDeterministicNumberOfRuns)
            initializeUnweightedScenarios();
        else
            initializeWeightedScenarios();

        long startTime = System.currentTimeMillis();

        if (synchronizedScheduling)
            runWithSynchronizedScheduling();
        else
            runWithAdHocScheduling();

        waitForCoolDownPeriod();

        totalRunTimeMilliSeconds = System.currentTimeMillis() - startTime;

    }

    private void initializeUnweightedScenarios() {
        scenariosList = new ArrayList<ScenarioBase>(scenarioWeightings.size());
        for (ScenarioBase scenario : scenarioWeightings.keySet())
            scenariosList.add(scenario);
    }

    /**
     * Get the overall rate of success for the executed scenarios.
     * 
     * @return the success percentage as a value between 0 and 100.
     */
    public double getSuccessRate() {
        return 100.0 * scenarioSuccesses.size() / numberOfRuns;
    }

    /**
     * Get information about the successful scenarios.
     * 
     * @return a map keyed on scenario id, containing information about the successful scenarios.
     */
    public Map<String, ScenarioResult> getScenarioSuccesses() {
        return scenarioSuccesses;
    }

    /**
     * Get information about the failed scenarios
     * 
     * @return a map keyed on scenario id, containing information about the failed scenarios.
     */
    public Map<String, ScenarioResult> getScenarioFailures() {
        return scenarioFailures;
    }

    /**
     * Get the total run time
     * 
     * @return the total time, in milliseconds, taken to run all scenarios, including the cool down period
     */
    public long getTotalRunTimeMilliSeconds() {
        return totalRunTimeMilliSeconds;
    }

    /**
     * Report a scenario as failed to the runner.
     * 
     * @param scenarioId
     *            the id of the failed scenario.
     * @param message
     *            a message describing the failed scenario. This could either be the reason for failure, or the last
     *            known successful state.
     */
    public void scenarioFailure(String scenarioId, String message) {
        ScenarioResult result = scenarioFailures.get(scenarioId);
        log.info(String.format("Scenario %s (%s) failed", scenarioId, result.getScenarioType().getSimpleName()));
        setResults(result, message);

        notifyCountDownLatch();
    }

    /**
     * Report a scenario as successful to the runner.
     * 
     * @param scenarioId
     *            the id of the successful scenario.
     */
    public void scenarioSuccess(String scenarioId) {
        ScenarioResult result = scenarioFailures.remove(scenarioId);
        log.info(String.format("Scenario %s (%s) succeeded", scenarioId, result.getScenarioType().getSimpleName()));
        setResults(result, null);
        scenarioSuccesses.put(scenarioId, result);

        notifyCountDownLatch();
    }

    private void setResults(ScenarioResult result, String message) {
        result.setEndTime(System.currentTimeMillis());
        result.setMessage(message);
    }

    private void notifyCountDownLatch() {
        if (scenarioCompleteLatch != null)
            scenarioCompleteLatch.countDown();
    }

    private void initializeWeightedScenarios() {
        weightedScenariosList = new ArrayList<ScenarioBase>(HUNDRED);
        int cumulativeWeights = 0;
        for (Integer weight : scenarioWeightings.values())
            cumulativeWeights += weight;
        double normalizationFactor = 1.0 * HUNDRED / cumulativeWeights;
        for (ScenarioBase scenario : scenarioWeightings.keySet()) {
            double normalizedWeight = normalizationFactor * scenarioWeightings.get(scenario);
            for (int i = 0; i < normalizedWeight; i++)
                weightedScenariosList.add(scenario);
        }
    }

    private void runWithAdHocScheduling() {
        for (int i = 0; i < numberOfRuns; i++) {
            ScenarioBase scenario = getNextScenario();
            String scenarioId = getScenarioId(i, scenario);

            scenarioFailures.get(scenarioId).setStartTime(System.currentTimeMillis());
            runScenario(scenario, scenarioId);
        }
    }

    private void runWithSynchronizedScheduling() {
        for (int i = 0; i < numberOfRuns; i += numberOfConcurrentStarts) {
            ScenarioBase scenario = null;
            String scenarioId = null;
            scenarioStartLatch = new CountDownLatch(numberOfConcurrentStarts);
            scenarioCompleteLatch = new CountDownLatch(numberOfConcurrentStarts);

            for (int j = 0; j < numberOfConcurrentStarts; j++) {
                Map<String, ScenarioBase> concurrentStarts = new HashMap<String, ScenarioBase>(numberOfConcurrentStarts);
                scenario = getNextScenario();
                scenarioId = getScenarioId(i + j, scenario);
                concurrentStarts.put(scenarioId, scenario);
                runSynchronizedScenario(scenario, scenarioId);
                scenarioStartLatch.countDown();
            }

            try {
                scenarioCompleteLatch.await(synchronizeWaitMilliSeconds, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                log.error(String.format("Interrupted while waiting for latch"), e);
            }
        }
    }

    protected ScenarioBase getNextScenario() {
        ScenarioBase scenario = useDeterministicNumberOfRuns ? getOrderedScenario() : getRandomScenario();
        scenario.setScenarioListener(this);
        scenario.setStartLatch(scenarioStartLatch);
        return scenario;
    }

    protected ScenarioBase getOrderedScenario() {
        return scenariosList.get(scenariosCounter++ % scenariosList.size());
    }

    protected ScenarioBase getRandomScenario() {
        return weightedScenariosList.get(randomGenerator.nextInt(HUNDRED));
    }

    private String getScenarioId(int index, final ScenarioBase scenario) {
        String scenarioId = String.format("%d", index);
        scenarioFailures.put(scenarioId, new ScenarioResult(scenario.getClass(), "Failed to complete"));
        return scenarioId;
    }

    private void runScenario(final ScenarioBase scenario, final String scenarioId) {
        RunnableScenario runnableScenario = new RunnableScenario(scenario, scenarioId);
        scheduledExecutorService.execute(runnableScenario);
    }

    private void runSynchronizedScenario(final ScenarioBase scenario, final String scenarioId) {
        RunnableSynchronizedScenario runnableScenario = new RunnableSynchronizedScenario(scenario, scenarioId);
        scheduledExecutorService.execute(runnableScenario);
    }

    private void waitForCoolDownPeriod() {
        try {
            Thread.sleep(coolDownPeriod);
        } catch (InterruptedException e) {
            log.error("Interrupted while cooling down at the end", e);
        }
    }

    /**
     * Set numberBeforeRuns before execution.
     * 
     * @param numberOfRuns
     *            the total number of scenarios to execute. Default is 500.
     */
    public void setNumberOfRuns(int numberOfRuns) {
        this.numberOfRuns = numberOfRuns;
    }

    /**
     * Set numberOfConcurrentStarts before execution.
     * 
     * @param numberOfConcurrentStarts
     *            the number of threads to be used for concurrent execution of scenarios. Default is 5.
     */
    public void setNumberOfConcurrentStarts(int numberOfConcurrentStarts) {
        this.numberOfConcurrentStarts = numberOfConcurrentStarts;
    }

    /**
     * Set scenarioWeightings before execution
     * 
     * @param scenarioWeightings
     *            a map keyed on the scenarios to execute, along with a relative weighting, represented by an integer.
     *            Default is null.
     */
    public void setScenarioWeightings(Map<ScenarioBase, Integer> scenarioWeightings) {
        this.scenarioWeightings = scenarioWeightings;
    }

    /**
     * Set scheduledExecutorService before execution
     * 
     * @param scheduledExecutorService
     *            an ExecutorService with a thread pool that can be used to submit scenarios for execution. A simple
     *            implementation that can be used for this purpose can be got by new
     *            java.util.concurrent.ScheduledThreadPoolExecutor(<thread pool size>). Default is null.
     */
    public void setScheduledExecutorService(ScheduledExecutorService scheduledExecutorService) {
        this.scheduledExecutorService = scheduledExecutorService;
    }

    /**
     * Set coolDownPeriod before execution
     * 
     * @param coolDownPeriod
     *            the duration, in milliseconds, that the runner should wait after initiating execution of the final
     *            scenario for all scenarios to complete. If a scenario does not complete within the coolDownPeriod, it
     *            is marked as a failure. Default is 1000.
     */
    public void setCoolDownPeriod(long coolDownPeriod) {
        this.coolDownPeriod = coolDownPeriod;
    }

    /**
     * Set synchronizedScheduling before execution
     * 
     * @param synchronizedScheduling
     *            a boolean, which if set to true, means that when a thread executes one scenario, it does not
     *            immediately start the next scenario until the other threads in the pool have finished execution of
     *            their respective scenarios. This guarantees that the system under test will always have to initiate
     *            the same number of scenarios in parallel. Alternatively, if this value is set to false, a thread will
     *            move on to the execution of the next scenario independent of other threads; this offers a more varied
     *            test of the system and probably a different set of race conditions. Default is false.
     */
    public void setSynchronizedScheduling(boolean synchronizedScheduling) {
        this.synchronizedScheduling = synchronizedScheduling;
    }

    /**
     * Set synchronizeWaitMilliSeconds
     * 
     * @param synchronizeWaitMilliSeconds
     *            the maximum duration, in milliseconds, that a thread should wait for other threads to finish execution
     *            of their respective scenarios to synchronize the start of the next scenario before assuming the other
     *            threads to be infinite and progressing with its next scenario. Default is 1000.
     */
    public void setSynchronizeWaitMilliSeconds(int synchronizeWaitMilliSeconds) {
        this.synchronizeWaitMilliSeconds = synchronizeWaitMilliSeconds;
    }

    /**
     * Set useDeterminnisticNumberOfRuns
     * 
     * @param useDeterministicNumberOfRuns
     *            a boolean, if set to true, means that the scenarios will be treated as being weighted equally and will
     *            guaranteed to be executed an equal number of times. Setting this to true overrides any weightings.
     *            Default is false.
     */
    public void setUseDeterministicNumberOfRuns(boolean useDeterministicNumberOfRuns) {
        this.useDeterministicNumberOfRuns = useDeterministicNumberOfRuns;
    }

    private static class RunnableScenario implements Runnable {
        ScenarioBase scenario;
        String scenarioId;

        public RunnableScenario(ScenarioBase aScenario, String aScenarioId) {
            scenario = aScenario;
            scenarioId = aScenarioId;
        }

        public void run() {
            scenario.run(scenarioId);
        }
    }

    private static class RunnableSynchronizedScenario implements Runnable {
        ScenarioBase scenario;
        String scenarioId;

        public RunnableSynchronizedScenario(ScenarioBase aScenario, String aScenarioId) {
            scenario = aScenario;
            scenarioId = aScenarioId;
        }

        public void run() {
            scenario.runSynchronized(scenarioId);
        }
    }
}
