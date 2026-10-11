package com.cyberopsarena.simulation.strategy;

import org.springframework.stereotype.Component;
@Component public class LateralMovementStrategy implements AttackStrategy {
 public String stepType(){return "LATERAL_MOVEMENT";}
 public StepEffect execute(java.util.Map<String,Object> preconditions){return new StepEffect(stepType(),null,true,0);}
}
