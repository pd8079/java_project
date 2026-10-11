package com.cyberopsarena.simulation.strategy;

import org.springframework.stereotype.Component;
@Component public class SuspiciousLoginStrategy implements AttackStrategy {
 public String stepType(){return "SUSPICIOUS_LOGIN";}
 public StepEffect execute(java.util.Map<String,Object> preconditions){Object attempts=preconditions.getOrDefault("attempts",5);if(!(attempts instanceof Number n)||n.intValue()<1||n.intValue()>100)throw new IllegalArgumentException("Login attempts must be between 1 and 100");return new StepEffect(stepType(),null,false,n.intValue());}
}
