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

/**
 * This class encapsulates the details of each scenario that was executed. All of the information is updated by the
 * Runner class so that the testing system can collect and present detailed information about each scenario.
 */
public class ScenarioResult {
    private Class<?> scenarioType;
    private long startTime;
    private long endTime;
    private String message;

    /**
     * Constructor, used by Runner, to record details of each scenario.
     */
    public ScenarioResult() {
        this(null, null);
    }

    /**
     * Constructor, used by Runner, to record details of each scenario.
     * 
     * @param scenarioType
     *            the class that defines the scenario.
     * @param message
     *            defaults to an error message, until the scenario is marked as complete.
     */
    public ScenarioResult(Class<?> scenarioType, String message) {
        this.scenarioType = scenarioType;
        this.message = message;
    }

    /**
     * Get the status message for the scenario, or the final message if the scenario has already finished.
     * 
     * @return the status message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Update the status message during (and after) a scenario's execution
     * 
     * @param message
     *            the status message
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Get the start time of the scenario when it starts execution
     * 
     * @return the start time
     */
    public long getStartTime() {
        return this.startTime;
    }

    /**
     * Set the start time of the scenario when it starts execution
     * 
     * @param startTime
     *            the start time
     */
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    /**
     * Set the end time of the scenario when it finishes execution
     * 
     * @param endTime
     *            the end time
     */
    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    /**
     * Get the end time of the scenario when it finishes execution
     * 
     * @return the end time
     */
    public long getEndTime() {
        return this.endTime;
    }

    /**
     * Return the duration of the scenario
     * 
     * @return the duration, in milliseconds
     */
    public long getDuration() {
        return endTime - startTime;
    }

    /**
     * Return the type of scenario that this object represents
     * 
     * @return the class of the scenario
     */
    public Class<?> getScenarioType() {
        return scenarioType;
    }

    /**
     * Set the type of scenario that this object represents
     * 
     * @param clazz
     *            the class of the scenario
     */
    public void setScenarioType(Class<?> clazz) {
        this.scenarioType = clazz;
    }
}
