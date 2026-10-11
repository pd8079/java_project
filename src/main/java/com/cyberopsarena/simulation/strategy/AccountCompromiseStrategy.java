package com.cyberopsarena.simulation.strategy;

import org.springframework.stereotype.Component;
@Component public class AccountCompromiseStrategy implements AttackStrategy {
 public String stepType(){return "ACCOUNT_COMPROMISE";}
 public StepEffect execute(java.util.Map<String,Object> preconditions){return new StepEffect(stepType(),"COMPROMISED",false,0);}
}
