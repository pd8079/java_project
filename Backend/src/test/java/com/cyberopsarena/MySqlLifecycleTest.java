package com.cyberopsarena;

import com.cyberopsarena.controller.SimulationController.DefenseRequest;
import com.cyberopsarena.repository.UserJdbcRepository;
import com.cyberopsarena.service.BackgroundSimulationRunner;
import com.cyberopsarena.service.SimulationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker=true)
class MySqlLifecycleTest {
 @Container static final MySQLContainer<?> MYSQL=new MySQLContainer<>("mysql:8.4").withDatabaseName("arena_test").withUsername("arena").withPassword("arena-test-password");
 @DynamicPropertySource static void db(DynamicPropertyRegistry r){r.add("spring.datasource.url",MYSQL::getJdbcUrl);r.add("spring.datasource.username",MYSQL::getUsername);r.add("spring.datasource.password",MYSQL::getPassword);r.add("app.jwt-secret",()->"test-only-secret-that-is-long-enough-for-hmac");}
 @Autowired JdbcTemplate jdbc;@Autowired SimulationService simulations;
 @Autowired UserJdbcRepository users;@Autowired BackgroundSimulationRunner backgroundRunner;
 @Autowired TestRestTemplate http;
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

 @Test void userJdbcRepositoryPerformsPreparedStatementCrud(){
   long id=users.create("jdbc-crud-user","jdbc-crud@test.local","test-hash","ANALYST");
   assertEquals("jdbc-crud-user",users.findById(id).orElseThrow().get("username"));
   assertTrue(users.update(id,"jdbc-crud-renamed","jdbc-crud-renamed@test.local","DEFENDER"));
   assertTrue(users.updateEnabled(id,false));
   var updated=users.findById(id).orElseThrow();
   assertEquals("jdbc-crud-renamed",updated.get("username"));
   assertEquals("DEFENDER",updated.get("role"));
   assertEquals(false,updated.get("enabled"));
   assertTrue(users.deleteById(id));
   assertTrue(users.findById(id).isEmpty());
 }

 @Test void backgroundRunnableCompletesScenarioOnWorkerThread() throws InterruptedException {
   long id=((Number)simulations.createRun(1,"learner").get("id")).longValue();
   backgroundRunner.execute(id,"learner");
   long deadline=System.nanoTime()+15_000_000_000L;
   while("CREATED".equals(simulations.run(id,"learner").get("status"))||"RUNNING".equals(simulations.run(id,"learner").get("status"))){
     if(System.nanoTime()>deadline)fail("Background simulation did not finish within 15 seconds");
     Thread.sleep(20);
   }
   assertEquals("COMPLETED",simulations.run(id,"learner").get("status"));
   assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM simulation_events WHERE run_id=?",Integer.class,id));
 }

 @Test void startsLiveHttpApplicationWithHealthyDatabase() {
   var response=http.getForEntity("/actuator/health",String.class);
   assertEquals(200,response.getStatusCode().value());
   assertTrue(response.getBody().contains("\"status\":\"UP\""));
 }
}
