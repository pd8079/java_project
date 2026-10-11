package com.cyberopsarena.controller;

import com.cyberopsarena.service.SimulationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@RestController @RequestMapping("/api/v1/scenarios")
public class ScenarioController {
 private final JdbcTemplate jdbc;private final SimulationService simulations;
 public ScenarioController(JdbcTemplate jdbc,SimulationService simulations){this.jdbc=jdbc;this.simulations=simulations;}
 public record ScenarioRequest(@NotBlank String name,String description){}
 public record StepRequest(@Positive int stepOrder,@NotBlank String stepType,Long sourceAssetId,Long targetAssetId,Map<String,Object> preconditions,Map<String,Object> effects){}
 @GetMapping public Object list(){return simulations.scenarios();}
 @GetMapping("/{id}") public Object get(@PathVariable long id){return simulations.scenario(id);}
 @PostMapping @Transactional @PreAuthorize("hasAnyRole('ADMIN','ANALYST')") public Object create(@Valid @RequestBody ScenarioRequest req,Authentication auth){long actor=jdbc.queryForObject("SELECT id FROM users WHERE username=?",Long.class,auth.getName());jdbc.update("INSERT INTO attack_scenarios(name,description,version,enabled,created_by) VALUES(?,?,1,TRUE,?)",req.name(),req.description(),actor);long id=jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class);audit(actor,"SCENARIO_CREATED",id);return get(id);}
 @PostMapping("/{id}/steps") @Transactional @PreAuthorize("hasAnyRole('ADMIN','ANALYST')") public Object addStep(@PathVariable long id,@Valid @RequestBody StepRequest req,Authentication auth){
   var scenario=jdbc.query("SELECT id FROM attack_scenarios WHERE id=? AND enabled=TRUE FOR UPDATE",(r,n)->r.getLong(1),id);if(scenario.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Scenario not found");
   if(jdbc.queryForObject("SELECT COUNT(*) FROM simulation_runs WHERE scenario_id=?",Integer.class,id)>0)throw new ResponseStatusException(HttpStatus.CONFLICT,"Scenario is immutable after a run has been created");
   Set<String> types=Set.of("ACCOUNT_COMPROMISE","LATERAL_MOVEMENT","SUSPICIOUS_LOGIN");if(!types.contains(req.stepType()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unsupported scenario step type");
   Map<String,Object> pre=req.preconditions()==null?Map.of():req.preconditions(),effects=req.effects()==null?Map.of():req.effects();if(!effects.isEmpty())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"This step type does not accept caller-supplied effects");if("SUSPICIOUS_LOGIN".equals(req.stepType())){if(!Set.of("attempts").containsAll(pre.keySet())||!(pre.getOrDefault("attempts",5) instanceof Number attempts)||attempts.intValue()<1||attempts.intValue()>100)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Suspicious login attempts must be an integer from 1 to 100");}else if(!pre.isEmpty())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"This step type does not accept preconditions");
   if(req.sourceAssetId()==null||req.targetAssetId()==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Source and target assets are required");
   var assets=jdbc.query("SELECT id,template_id FROM assets WHERE id IN (?,?)",(r,n)->Map.of(r.getLong(1),r.getLong(2)),req.sourceAssetId(),req.targetAssetId());if(assets.size()!=2)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Scenario assets must exist");
   long sourceTemplate=assets.getFirst().get(req.sourceAssetId()),targetTemplate=assets.getFirst().get(req.targetAssetId());if(sourceTemplate!=targetTemplate)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Scenario assets must belong to the same template");
   Integer existing=jdbc.queryForObject("SELECT COUNT(*) FROM scenario_steps ss JOIN assets a ON a.id=ss.source_asset_id WHERE ss.scenario_id=? AND a.template_id<>?",Integer.class,id,sourceTemplate);if(existing!=null&&existing>0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"All steps in a scenario must use one template");
   jdbc.update("INSERT INTO scenario_steps(scenario_id,step_order,step_type,source_asset_id,target_asset_id,preconditions,effects) VALUES(?,?,?,?,?,CAST(? AS JSON),CAST(? AS JSON))",id,req.stepOrder(),req.stepType(),req.sourceAssetId(),req.targetAssetId(),toJson(pre),toJson(effects));
   jdbc.update("UPDATE attack_scenarios SET version=version+1 WHERE id=?",id);long actor=jdbc.queryForObject("SELECT id FROM users WHERE username=?",Long.class,auth.getName());audit(actor,"SCENARIO_STEP_CREATED",id);return get(id);
 }
 private void audit(long actor,String action,long id){jdbc.update("INSERT INTO audit_logs(actor_id,action,entity_type,entity_id,details) VALUES(?,?, 'attack_scenarios',?,JSON_OBJECT())",actor,action,id);}
 private String toJson(Object value){try{return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(value);}catch(Exception e){throw new IllegalArgumentException(e);}}
}
