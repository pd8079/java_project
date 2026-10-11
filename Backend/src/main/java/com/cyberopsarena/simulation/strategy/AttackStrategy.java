package com.cyberopsarena.simulation.strategy;

import java.util.Map;

/** An allowlisted, deterministic transformation for a supported virtual scenario step. */
public interface AttackStrategy {
 String stepType();
 StepEffect execute(Map<String,Object> preconditions);
 record StepEffect(String eventType,String successfulTargetState,boolean requiresReachability,int simulatedFailedLogins) {}
}
