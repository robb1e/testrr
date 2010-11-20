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

import java.util.concurrent.CountDownLatch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Base class that every scenario should extend. This class provides some basic constructs that are common to all scenarios.
 * Each class that extends this base class should represent a "type" of scenario. It should ideally be a thread-safe, singleton class,
 * able to store the execution states of multiple scenario instances.  
 */
public abstract class ScenarioBase {
	private static final Log log = LogFactory.getLog(ScenarioBase.class);

	private ScenarioListener scenarioListener;
	private CountDownLatch startLatch = null;

	/**
	 * Returns the ScenarioListener, typically an instance of the Runner class. Scenarios will need to notify ScenarioListener about
	 * the success or failure of each scenario.
	 * @return the ScenarioListener.
	 */
	public ScenarioListener getScenarioListener() {
		return scenarioListener;
	}

	/**
	 * Used by the Runner class to set itself as the ScenarioListener for every scenario it creates.
	 * @param scenarioListener the ScenarioListener (Runner)
	 */
	public void setScenarioListener(ScenarioListener scenarioListener) {
		this.scenarioListener = scenarioListener;
	}
	
	/**
	 * Used by the Runner class in synchronizedScheduling mode.
	 * @param latch the latch to wait on to kick off a scenario.
	 */
	public void setStartLatch(CountDownLatch latch) {
		this.startLatch = latch;
	}
	
	private void awaitBarrier(String scenarioId) {
		try {
			startLatch.await();
		} catch (InterruptedException e) {
			log.error(String.format("Scenario %s interrupted while waiting for latch", scenarioId), e);
		}
	}
	
	/**
	 * Used by the Runner class in synchronizedScheduling mode.
	 * @param scenarioId the id of the scenario to execute.
	 */
	public void runSynchronized(String scenarioId) {
		awaitBarrier(scenarioId);
		run(scenarioId);
	}

	/**
	 * The run method starts execution of the specific scenario.
	 * @param scenarioId the id of the scenario to execute.
	 */
	public abstract void run(String scenarioId);
}
