package com.cyberopsarena.simulation.strategy;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AttackStrategyRegistryTest {
 private final AttackStrategyRegistry registry=new AttackStrategyRegistry(List.of(new AccountCompromiseStrategy(),new LateralMovementStrategy(),new SuspiciousLoginStrategy()));
 @Test void resolvesOnlyKnownStepTypes(){assertTrue(registry.find("ACCOUNT_COMPROMISE").isPresent());assertTrue(registry.find("client.ClassName").isEmpty());}
 @Test void strategiesReturnDeterministicEffects(){assertEquals("COMPROMISED",registry.find("ACCOUNT_COMPROMISE").orElseThrow().execute(java.util.Map.of()).successfulTargetState());assertTrue(registry.find("LATERAL_MOVEMENT").orElseThrow().execute(java.util.Map.of()).requiresReachability());assertEquals(7,registry.find("SUSPICIOUS_LOGIN").orElseThrow().execute(java.util.Map.of("attempts",7)).simulatedFailedLogins());}
}
