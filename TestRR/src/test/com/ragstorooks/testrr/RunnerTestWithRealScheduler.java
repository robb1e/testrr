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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.junit.Before;
import org.junit.Test;

import com.ragstorooks.testrr.Runner;
import com.ragstorooks.testrr.ScenarioBase;
import com.ragstorooks.testrr.ScenarioResult;


public class RunnerTestWithRealScheduler {
	private Runner runner;
	private Map<ScenarioBase, Integer> scenarioWeightings;
	private ScheduledThreadPoolExecutor executorService;
	
	private final Map<String, ScenarioBase> scenarios = new HashMap<String, ScenarioBase>();
	
	private class MyRPScenario1 extends ScenarioBase {
		// Synchronous method taking 50ms to execute its work
		@Override
		public void run(String scenarioId) {
			scenarios.put(scenarioId, this);
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			getScenarioListener().scenarioSuccess(scenarioId);
		}
	}
	
	private class MyRPScenario2 extends ScenarioBase {
		// Asynchronous method taking 1 second to do its work
		@Override
		public void run(final String scenarioId) {
			scenarios.put(scenarioId, this);
			new Timer().schedule(new TimerTask() {
				@Override
				public void run() {
					getScenarioListener().scenarioSuccess(scenarioId);
				}
			}, 1000);
		}
	}
	
	@Before
	public void before() {
		scenarioWeightings = new HashMap<ScenarioBase, Integer>();
		scenarioWeightings.put(new MyRPScenario1(), 1);
		scenarioWeightings.put(new MyRPScenario2(), 1);
		
		executorService = new ScheduledThreadPoolExecutor(4);

		runner = new Runner();
		runner.setNumberOfConcurrentStarts(5);
		runner.setNumberOfRuns(20);
		runner.setCoolDownPeriod(1500);
		runner.setScenarioWeightings(scenarioWeightings);
		runner.setScheduledExecutorService(executorService);
	}
	
	@Test
	public void testAdHocScheduling() throws Exception {
		// setup
		long startTime = System.currentTimeMillis();
		
		// act
		runner.run();
		
		// assert
		long endTime = System.currentTimeMillis();
		assertEquals(String.format("Failures list: %s", runner.getScenarioFailures().keySet()), 20, runner.getScenarioSuccesses().size());
		assertTrue(endTime - startTime <= 5000);
	}
	
	@Test
	public void testSynchronizedScheduling() throws Exception {
		// setup
		runner.setSynchronizedScheduling(true);
		runner.setSynchronizeWaitMilliSeconds(1500);
		long startTime = System.currentTimeMillis();
		
		// act
		runner.run();
		
		// assert
		long endTime = System.currentTimeMillis();
		assertEquals(String.format("Failures list: %s", runner.getScenarioFailures().keySet()), 20, runner.getScenarioSuccesses().size());
		assertTrue(endTime - startTime > 5000);
	}
	
	@Test
	public void testDurationWithAdHocSchedulingByScenario() throws Exception {
		// setup
		
		// act
		runner.run();
		
		// assert
		Map<String, ScenarioResult> successfulScenarios = runner.getScenarioSuccesses();
		for (String scenarioId : successfulScenarios.keySet()) {
			ScenarioResult result = successfulScenarios.get(scenarioId);
			if (scenarios.get(scenarioId) instanceof MyRPScenario1)
				assertTrue(String.format("%d", result.getDuration()), result.getDuration() < 1000);
			else
				assertTrue(String.format("%d", result.getDuration()), result.getDuration() >= 1000);
		}
	}
}
