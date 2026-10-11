package com.cyberopsarena.simulation.strategy;

import org.springframework.stereotype.Component;
import java.util.*;

/** Maps data-defined step types to known code. Client input is never used as a class name. */
@Component public class AttackStrategyRegistry {
 private final Map<String,AttackStrategy> strategies;
 public AttackStrategyRegistry(List<AttackStrategy> strategies){Map<String,AttackStrategy> map=new HashMap<>();for(var strategy:strategies){if(map.put(strategy.stepType(),strategy)!=null)throw new IllegalStateException("Duplicate attack strategy: "+strategy.stepType());}this.strategies=Map.copyOf(map);}
 public Optional<AttackStrategy> find(String type){return Optional.ofNullable(strategies.get(type));}
}
