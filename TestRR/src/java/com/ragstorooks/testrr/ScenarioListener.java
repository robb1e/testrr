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
 * An interface used to record the success or failure of any given scenario. This is implemented by the Runner class, and as such,
 * need not be used by any scenario, except to notify the status of a scenario.
 */
public interface ScenarioListener {
    /**
     * Report a scenario as successful to the listener.
     * @param scenarioId the id of the successful scenario.
     */
	void scenarioSuccess(String scenarioId);
	
    /**
     * Report a scenario as failed to the listener.
     * @param scenarioId the id of the failed scenario.
     * @param message a message describing the failed scenario. This could either be the reason for failure, or the last known successful state. 
     */
	void scenarioFailure(String scenarioId, String message);
}
