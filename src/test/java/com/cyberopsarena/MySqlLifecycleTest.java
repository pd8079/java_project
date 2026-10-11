package com.cyberopsarena;

import com.cyberopsarena.controller.SimulationController.DefenseRequest;
import com.cyberopsarena.service.SimulationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers(disabledWithoutDocker=true)
class MySqlLifecycleTest {
 @Container static final MySQLContainer<?> MYSQL=new MySQLContainer<>("mysql:8.4").withDatabaseName("arena_test").withUsername("arena").withPassword("arena-test-password");
 @DynamicPropertySource static void db(DynamicPropertyRegistry r){r.add("spring.datasource.url",MYSQL::getJdbcUrl);r.add("spring.datasource.username",MYSQL::getUsername);r.add("spring.datasource.password",MYSQL::getPassword);r.add("app.jwt-secret",()->"test-only-secret-that-is-long-enough-for-hmac");}
 @Autowired JdbcTemplate jdbc;@Autowired SimulationService simulations;
 @BeforeEach void ensureUsers(){jdbc.update("INSERT IGNORE INTO users(username,email,password_hash,role,enabled) VALUES('learner','learner@test.local','not-used','ANALYST',TRUE),('defender','defender@test.local','not-used','DEFENDER',TRUE)");}
 @Test void migratesAndRunsIsolatedScenarioWithEventAlertAndDefenseHistory(){
   var run=simulations.createRun(1,"learner");long id=((Number)run.get("id")).longValue();
   simulations.start(id,"learner");simulations.step(id,"learner");
   var account=jdbc.queryForObject("SELECT id FROM assets WHERE name='finance-database'",Long.class);
   var action=simulations.defend(id,"defender",new DefenseRequest("ISOLATE_ASSET","Protect critical data",account,null,null));
   assertEquals("APPLIED",action.get("status"));
   simulations.step(id,"learner");
   assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM simulation_events WHERE run_id=?",Integer.class,id));
   assertEquals("BLOCKED",jdbc.queryForObject("SELECT outcome FROM simulation_events WHERE run_id=? ORDER BY sequence_number DESC LIMIT 1",String.class,id));
   assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM alerts WHERE run_id=?",Integer.class,id));
   assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM defense_actions WHERE run_id=? AND status='APPLIED'",Integer.class,id));
   assertTrue(jdbc.queryForObject("SELECT isolated FROM simulation_run_assets WHERE run_id=? AND asset_id=?",Boolean.class,id,account));
 }
}
