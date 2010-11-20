package com.ragstorooks.samples;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.ragstorooks.testrr.Runner;
import com.ragstorooks.testrr.ScenarioBase;
import com.ragstorooks.testrr.ScenarioResult;

/*
 * This sample loads a Spring Application Context containing the definitions and weightings of two robustness scenarios and executes them
 */
public class Main {
	public static void main(String[] args) {
		// load application context and configure beans
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("applicationContext.xml");
		ClassPathXmlApplicationContext mockphoneContext = null;
		Runner runner = null;
		
		try {
			mockphoneContext = new ClassPathXmlApplicationContext("mockphoneApplicationContext.xml");
			Iterator<?> iterator = applicationContext.getBeansOfType(Runner.class).values().iterator();
			if (iterator.hasNext())
				runner = (Runner)iterator.next();
			
			Map<ScenarioBase, Integer> scenarioWeightings = new HashMap<ScenarioBase, Integer>();
			iterator = applicationContext.getBeansOfType(ScenarioBase.class).values().iterator();
			while (iterator.hasNext())
				scenarioWeightings.put((ScenarioBase)iterator.next(), 1);
	
			runner.setScenarioWeightings(scenarioWeightings);
			
			// invoke the runner and view statistics
			runner.run();
		} finally {
			// destroy application context and end program
			if (mockphoneContext != null) mockphoneContext.destroy();
			applicationContext.destroy();
		}

		Map<String, ScenarioResult> failures = runner.getScenarioFailures();
		Iterator<?> iterator = failures.keySet().iterator();
		while (iterator.hasNext()) {
			String scenarioId = (String)iterator.next();
			ScenarioResult result = failures.get(scenarioId);
			System.out.println(String.format("Scenario %s(%s) failed because: %s", result.getScenarioType().getSimpleName(), scenarioId, result.getMessage()));
		}
		System.out.println(String.format("Total time taken to run scenarios: %d milliseconds", runner.getTotalRunTimeMilliSeconds()));
		System.out.println(String.format("Success rate overall: %f percent", runner.getSuccessRate()));
	}
}
