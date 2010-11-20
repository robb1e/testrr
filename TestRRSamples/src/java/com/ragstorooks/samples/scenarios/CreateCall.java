package com.ragstorooks.samples.scenarios;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.bt.aloha.call.CallBean;
import com.bt.aloha.call.CallListener;
import com.bt.aloha.call.event.CallConnectedEvent;
import com.bt.aloha.call.event.CallConnectionFailedEvent;
import com.bt.aloha.call.event.CallDisconnectedEvent;
import com.bt.aloha.call.event.CallTerminatedEvent;
import com.bt.aloha.call.event.CallTerminationFailedEvent;
import com.bt.aloha.callleg.OutboundCallLegBean;
import com.ragstorooks.testrr.ScenarioBase;

public class CreateCall extends ScenarioBase implements CallListener {
	private Map<String, String> scenarios = new ConcurrentHashMap<String, String>();
	private Object lock = new Object();
	private OutboundCallLegBean outboundCallLegBean;
	private CallBean callBean;
	
	@Override
	public void run(String scenarioId) {
		String callLeg1 = outboundCallLegBean.createCallLeg(URI.create("sip:app"), URI.create("sip:happy@127.0.0.1"));
		String callLeg2 = outboundCallLegBean.createCallLeg(URI.create("sip:app"), URI.create("sip:happy@127.0.0.1"));
		String callId = callBean.joinCallLegs(callLeg1, callLeg2);
		scenarios.put(callId, scenarioId);
	}

	public void onCallConnected(CallConnectedEvent arg0) {
		String callId = arg0.getCallId();
		String scenarioId = scenarios.remove(callId);
		if (scenarioId != null) {
			callBean.terminateCall(callId);
			getScenarioListener().scenarioSuccess(scenarioId);
		}
	}

	public void onCallConnectionFailed(CallConnectionFailedEvent arg0) {
		String callId = arg0.getCallId();
		scenarios.remove(callId);
		getScenarioListener().scenarioFailure(callId, "Call Connection Failed Event received");
	}

	public void onCallDisconnected(CallDisconnectedEvent arg0) {
	}

	public void onCallTerminated(CallTerminatedEvent arg0) {
	}

	public void onCallTerminationFailed(CallTerminationFailedEvent arg0) {
	}

	public void setOutboundCallLegBean(OutboundCallLegBean outboundCallLegBean) {
		this.outboundCallLegBean = outboundCallLegBean;
	}

	public void setCallBean(CallBean callBean) {
		this.callBean = callBean;
		this.callBean.addCallListener(this);
	}
}
