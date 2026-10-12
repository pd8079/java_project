package com.cyberopsarena.service;

import com.cyberopsarena.controller.SimulationController.DefenseRequest;
import com.cyberopsarena.simulation.graph.NetworkGraph;
import com.cyberopsarena.simulation.strategy.AttackStrategyRegistry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@Service
public class SimulationService {
 private final JdbcTemplate jdbc; private final ObjectMapper json=new ObjectMapper();private final AttackStrategyRegistry strategies;
 public SimulationService(JdbcTemplate jdbc,AttackStrategyRegistry strategies){this.jdbc=jdbc;this.strategies=strategies;}
 public List<Map<String,Object>> scenarios(){return jdbc.queryForList("SELECT id,name,description,version,enabled,created_at FROM attack_scenarios WHERE enabled=TRUE ORDER BY id");}
 public Map<String,Object> scenario(long id){var s=jdbc.queryForMap("SELECT id,name,description,version,enabled,created_at FROM attack_scenarios WHERE id=?",id);s.put("steps",jdbc.queryForList("SELECT id,step_order,step_type,source_asset_id,target_asset_id,preconditions,effects FROM scenario_steps WHERE scenario_id=? ORDER BY step_order",id));return s;}
 @Transactional public Map<String,Object> createRun(long scenarioId,String username){
   var sc=jdbc.query("SELECT version FROM attack_scenarios WHERE id=? AND enabled=TRUE FOR UPDATE",(r,n)->r.getInt(1),scenarioId); if(sc.isEmpty())throw notFound("Enabled scenario not found");
   long user=userId(username); var templateId=jdbc.queryForObject("SELECT DISTINCT a.template_id FROM scenario_steps ss JOIN assets a ON a.id=ss.source_asset_id WHERE ss.scenario_id=? LIMIT 1",Long.class,scenarioId);
   if(templateId==null)throw bad("Scenario has no start asset");
   var assets=jdbc.queryForList("SELECT a.id,a.initial_state FROM assets a WHERE a.template_id=?",templateId);
   var edges=jdbc.queryForList("SELECT e.id FROM network_edges e WHERE e.template_id=? AND e.enabled=TRUE",templateId);
   String snapshot=write(Map.of("scenarioId",scenarioId,"scenarioVersion",sc.getFirst(),"templateId",templateId,"assets",assets,"edges",edges));
   jdbc.update("INSERT INTO simulation_runs(scenario_id,started_by,status,scenario_version,initial_snapshot) VALUES(?,?,'CREATED',?,CAST(? AS JSON))",scenarioId,user,sc.getFirst(),snapshot);
   long runId=jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class);
   jdbc.update("INSERT INTO simulation_run_assets(run_id,asset_id,current_state) SELECT ?,id,initial_state FROM assets WHERE template_id=?",runId,templateId);
   jdbc.update("INSERT INTO simulation_run_edges(run_id,network_edge_id,enabled) SELECT ?,id,enabled FROM network_edges WHERE template_id=?",runId,templateId);
   audit(user,"RUN_CREATED","simulation_runs",runId,Map.of("scenarioId",scenarioId)); return run(runId,username);
 }
 @Transactional public Map<String,Object> start(long id,String username){var r=lockedRun(id,username);String status=(String)r.get("status"); if("RUNNING".equals(status))return r;if(!"CREATED".equals(status)&&!"PAUSED".equals(status))throw bad("Run cannot be started from "+status);
   jdbc.update("UPDATE simulation_runs SET status='RUNNING',started_at=COALESCE(started_at,UTC_TIMESTAMP(6)) WHERE id=?",id);audit(userId(username),"RUN_STARTED","simulation_runs",id,Map.of());return run(id,username);}
 @Transactional public Map<String,Object> transition(long id,String username,String status){var r=lockedRun(id,username);String before=(String)r.get("status");if("PAUSED".equals(status)){if(!"RUNNING".equals(before))throw bad("Only a running simulation can be paused");}else if(!List.of("RUNNING","PAUSED","CREATED").contains(before))throw bad("Run is already terminal");
   jdbc.update("UPDATE simulation_runs SET status=?,finished_at=IF(?='STOPPED',UTC_TIMESTAMP(6),finished_at) WHERE id=?",status,status,id);audit(userId(username),"RUN_"+status,"simulation_runs",id,Map.of());return run(id,username);}
 @Transactional public void fail(long id,String username){var r=lockedRun(id,username);if(!List.of("CREATED","RUNNING").contains(r.get("status")))return;jdbc.update("UPDATE simulation_runs SET status='FAILED',finished_at=UTC_TIMESTAMP(6) WHERE id=?",id);audit(userId(username),"RUN_FAILED","simulation_runs",id,Map.of("reason","Background execution failed"));}
 @Transactional public Map<String,Object> step(long id,String username){
   var r=lockedRun(id,username);if(!"RUNNING".equals(r.get("status")))throw bad("Run must be started before stepping");int position=((Number)r.get("current_step")).intValue();long scenario=((Number)r.get("scenario_id")).longValue();
   var steps=jdbc.queryForList("SELECT id,step_order,step_type,source_asset_id,target_asset_id,preconditions,effects FROM scenario_steps WHERE scenario_id=? ORDER BY step_order",scenario);
   if(position>=steps.size()){jdbc.update("UPDATE simulation_runs SET status='COMPLETED',finished_at=UTC_TIMESTAMP(6) WHERE id=?",id);return run(id,username);}
   Map<String,Object> step=steps.get(position);String type=(String)step.get("step_type");Long source=num(step.get("source_asset_id")),target=num(step.get("target_asset_id"));String outcome="SUCCESS",detail="Simulated step completed";String eventType=type;
   var strategy=strategies.find(type);var effect=strategy.map(s->s.execute(jsonMap(step.get("preconditions")))).orElse(null);
   if(effect==null){outcome="BLOCKED";eventType="BLOCKED_TRANSITION";detail="Unsupported scenario step was rejected";}
   if(target!=null){var targetState=jdbc.query("SELECT ra.current_state,ra.isolated,ra.disabled,a.asset_type FROM simulation_run_assets ra JOIN assets a ON a.id=ra.asset_id WHERE ra.run_id=? AND ra.asset_id=? FOR UPDATE",(rs,n)->Map.of("state",rs.getString(1),"isolated",rs.getBoolean(2),"disabled",rs.getBoolean(3),"type",rs.getString(4)),id,target);if(targetState.isEmpty()){outcome="BLOCKED";eventType="BLOCKED_TRANSITION";detail="Target asset is not part of this run";}else if(Boolean.TRUE.equals(targetState.getFirst().get("isolated"))||Boolean.TRUE.equals(targetState.getFirst().get("disabled"))){outcome="BLOCKED";eventType="BLOCKED_TRANSITION";detail="Target asset is isolated or disabled in this run";}else if("ACCOUNT_COMPROMISE".equals(type)&&!"USER_ACCOUNT".equals(targetState.getFirst().get("type"))){outcome="BLOCKED";eventType="BLOCKED_TRANSITION";detail="Account compromise requires a simulated user account";}}
   if(source!=null&&"SUCCESS".equals(outcome)){var sourceState=jdbc.query("SELECT isolated,disabled FROM simulation_run_assets WHERE run_id=? AND asset_id=? FOR UPDATE",(rs,n)->Map.of("isolated",rs.getBoolean(1),"disabled",rs.getBoolean(2)),id,source);if(sourceState.isEmpty()){outcome="BLOCKED";eventType="BLOCKED_TRANSITION";detail="Source asset is not part of this run";}else if(Boolean.TRUE.equals(sourceState.getFirst().get("isolated"))||Boolean.TRUE.equals(sourceState.getFirst().get("disabled"))){outcome="BLOCKED";eventType="BLOCKED_TRANSITION";detail="Source asset is isolated or disabled in this run";}}
   if(effect!=null&&effect.requiresReachability()&&"SUCCESS".equals(outcome)&&source!=null&&target!=null){
     var edgeRows=jdbc.query("SELECT e.source_asset_id,e.target_asset_id FROM simulation_run_edges re JOIN network_edges e ON e.id=re.network_edge_id WHERE re.run_id=? AND re.enabled=TRUE AND re.blocked=FALSE",(rs,n)->new long[]{rs.getLong(1),rs.getLong(2)},id);
     NetworkGraph graph=new NetworkGraph(edgeRows); if(!graph.reachable(source,target)){outcome="BLOCKED";eventType="BLOCKED_TRANSITION";detail="No enabled simulated path connects the source and target";}
   }
   if(effect!=null&&effect.successfulTargetState()!=null&&"SUCCESS".equals(outcome)&&target!=null)jdbc.update("UPDATE simulation_run_assets SET current_state=? WHERE run_id=? AND asset_id=?",effect.successfulTargetState(),id,target);
   long seq=jdbc.queryForObject("SELECT COALESCE(MAX(sequence_number),0)+1 FROM simulation_events WHERE run_id=?",Long.class,id);
   String severity="HIGH".equals(type)||"BLOCKED".equals(outcome)?"HIGH":"MEDIUM";
   Map<String,Object> eventData=new LinkedHashMap<>();eventData.put("stepOrder",step.get("step_order"));eventData.put("description",detail);eventData.put("scenarioStepType",type);if(effect!=null&&effect.simulatedFailedLogins()>0)eventData.put("failedAttempts",effect.simulatedFailedLogins());
   jdbc.update("INSERT INTO simulation_events(run_id,sequence_number,event_type,source_asset_id,target_asset_id,outcome,severity,event_data) VALUES(?,?,?,?,?,?,?,CAST(? AS JSON))",id,seq,eventType,source,target,outcome,severity,write(eventData));
   long eventId=jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class);detect(id,eventId,eventType,target,severity);
   jdbc.update("UPDATE simulation_runs SET current_step=current_step+1 WHERE id=?",id);
   int count=jdbc.queryForObject("SELECT COUNT(*) FROM scenario_steps WHERE scenario_id=?",Integer.class,scenario);
   if(position+1>=count)jdbc.update("UPDATE simulation_runs SET status='COMPLETED',finished_at=UTC_TIMESTAMP(6) WHERE id=?",id);
   audit(userId(username),"SCENARIO_STEP","simulation_runs",id,Map.of("sequence",seq,"outcome",outcome));return run(id,username);
 }
 private void detect(long run,long event,String type,Long target,String severity){
   var rules=jdbc.queryForList("SELECT id,rule_type,threshold,window_seconds,severity,version FROM detection_rules WHERE enabled=TRUE AND event_type=?",type);
   for(var rule:rules){String ruleType=(String)rule.get("rule_type");boolean matches=true;String evidence="Event matched configured rule";
     if("REPEATED_FAILED_LOGIN".equals(ruleType)){Integer count=jdbc.queryForObject("SELECT COALESCE(SUM(CAST(JSON_UNQUOTE(JSON_EXTRACT(event_data,'$.failedAttempts')) AS UNSIGNED)),0) FROM simulation_events WHERE run_id=? AND event_type=? AND occurred_at>=UTC_TIMESTAMP(6)-INTERVAL ? SECOND",Integer.class,run,type,rule.get("window_seconds"));matches=count>=((Number)rule.get("threshold")).intValue();evidence="Matched "+count+" simulated failed login attempts in the configured window";}
     if("HIGH_CRITICALITY_ACCESS".equals(ruleType)&&target!=null){String criticality=jdbc.queryForObject("SELECT criticality FROM assets WHERE id=?",String.class,target);matches=List.of("HIGH","CRITICAL").contains(criticality);evidence="Target criticality: "+criticality;}
     if(matches)jdbc.update("INSERT IGNORE INTO alerts(run_id,rule_id,trigger_event_id,severity,status,evidence) VALUES(?,?,?,?,'OPEN',CAST(? AS JSON))",run,rule.get("id"),event,rule.get("severity"),write(Map.of("evidence",evidence,"ruleVersion",rule.get("version"),"eventId",event)));
   }
 }
 @Transactional public Map<String,Object> defend(long id,String username,DefenseRequest req){var auth=jdbc.queryForMap("SELECT id,role FROM users WHERE username=? AND enabled=TRUE",username);if(!List.of("ADMIN","DEFENDER").contains(auth.get("role")))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Defender role required");var r=lockedRun(id,username,"DEFENDER".equals(auth.get("role")));if(List.of("COMPLETED","STOPPED","FAILED").contains(r.get("status")))throw bad("Cannot defend a terminal run");
   String status="APPLIED";String result="";long actor=((Number)auth.get("id")).longValue();
   try { switch(req.actionType().toUpperCase(Locale.ROOT)){
     case "DISABLE_ACCOUNT" -> {int n=jdbc.update("UPDATE simulation_run_assets ra JOIN assets a ON a.id=ra.asset_id SET ra.disabled=TRUE,ra.current_state='DISABLED' WHERE ra.run_id=? AND ra.asset_id=? AND a.asset_type='USER_ACCOUNT' AND ra.disabled=FALSE",id,req.targetAssetId());if(n==0)throw bad("Target is not an enabled account in this run");result="Simulated account disabled";}
     case "ISOLATE_ASSET" -> {int n=jdbc.update("UPDATE simulation_run_assets SET isolated=TRUE WHERE run_id=? AND asset_id=? AND isolated=FALSE",id,req.targetAssetId());if(n==0)throw bad("Target is not an active asset in this run");result="Asset isolated in this run";}
     case "BLOCK_EDGE" -> {int n=jdbc.update("UPDATE simulation_run_edges SET blocked=TRUE WHERE run_id=? AND network_edge_id=? AND blocked=FALSE",id,req.edgeId());if(n==0)throw bad("Edge is not active in this run");result="Modeled edge blocked in this run";}
     case "ACKNOWLEDGE_ALERT" -> {if(req.alertId()==null)throw bad("alertId is required");var alert=jdbc.queryForObject("SELECT id FROM alerts WHERE id=? AND run_id=?",Long.class,req.alertId(),id);if(alert==null)throw notFound("Alert not found in run");jdbc.update("UPDATE alerts SET status='ACKNOWLEDGED',acknowledged_by=?,acknowledged_at=UTC_TIMESTAMP(6) WHERE id=? AND status='OPEN'",actor,req.alertId());result="Alert acknowledged";}
     default -> throw bad("Unsupported defense action");
   }}catch(ResponseStatusException ex){status="REJECTED";result=ex.getReason();jdbc.update("INSERT INTO defense_actions(run_id,actor_id,target_asset_id,action_type,status,reason,result) VALUES(?,?,?,?,?,?,CAST(? AS JSON))",id,actor,req.targetAssetId(),req.actionType(),status,req.reason(),write(Map.of("message",result)));long defenseId=jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class);audit(actor,"DEFENSE_REJECTED","simulation_runs",id,Map.of("action",req.actionType()));return jdbc.queryForMap("SELECT * FROM defense_actions WHERE id=?",defenseId);}
   jdbc.update("INSERT INTO defense_actions(run_id,actor_id,target_asset_id,action_type,status,reason,result) VALUES(?,?,?,?,?,?,CAST(? AS JSON))",id,actor,req.targetAssetId(),req.actionType(),status,req.reason(),write(Map.of("message",result)));
   long defenseId=jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class);audit(actor,"DEFENSE_APPLIED","simulation_runs",id,Map.of("action",req.actionType()));return jdbc.queryForMap("SELECT * FROM defense_actions WHERE id=?",defenseId);
 }
 @Transactional public Map<String,Object> alertStatus(long alertId,String username,String status){var a=jdbc.query("SELECT al.id,al.run_id,al.status FROM alerts al JOIN simulation_runs r ON r.id=al.run_id WHERE al.id=? AND (r.started_by=? OR EXISTS(SELECT 1 FROM users u WHERE u.username=? AND u.role='ADMIN'))",(rs,n)->Map.of("run",rs.getLong(2),"status",rs.getString(3)),alertId,userId(username),username);if(a.isEmpty())throw notFound("Alert not found");if("RESOLVED".equals(a.getFirst().get("status"))||"OPEN".equals(status)&&!"ACKNOWLEDGED".equals(a.getFirst().get("status")))throw bad("Invalid alert state transition");jdbc.update("UPDATE alerts SET status=?,acknowledged_by=IF(?='ACKNOWLEDGED',?,acknowledged_by),acknowledged_at=IF(?='ACKNOWLEDGED',UTC_TIMESTAMP(6),acknowledged_at) WHERE id=?",status,status,userId(username),status,alertId);return jdbc.queryForMap("SELECT * FROM alerts WHERE id=?",alertId);}
 public List<Map<String,Object>> runs(String username){long uid=userId(username);return jdbc.queryForList("SELECT id,scenario_id,started_by,status,scenario_version,current_step,started_at,finished_at,created_at,updated_at FROM simulation_runs WHERE started_by=? ORDER BY created_at DESC LIMIT 100",uid);}
 public Map<String,Object> run(long id,String username){var r=jdbc.query("SELECT id,scenario_id,started_by,status,scenario_version,current_step,started_at,finished_at,created_at,updated_at FROM simulation_runs WHERE id=?",(rs,n)->{Map<String,Object> m=new LinkedHashMap<>();for(String c:List.of("id","scenario_id","started_by","status","scenario_version","current_step","started_at","finished_at","created_at","updated_at"))m.put(c,rs.getObject(c));return m;},id);if(r.isEmpty())throw notFound("Run not found");if(!Objects.equals(((Number)r.getFirst().get("started_by")).longValue(),userId(username))&&!isAdmin(username))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Run access denied");return r.getFirst();}
 private Map<String,Object> lockedRun(long id,String username){return lockedRun(id,username,false);}
 private Map<String,Object> lockedRun(long id,String username,boolean defenderAccess){var r=jdbc.query("SELECT id,scenario_id,started_by,status,scenario_version,current_step FROM simulation_runs WHERE id=? FOR UPDATE",(rs,n)->{Map<String,Object> m=new HashMap<>();m.put("id",rs.getLong(1));m.put("scenario_id",rs.getLong(2));m.put("started_by",rs.getLong(3));m.put("status",rs.getString(4));m.put("scenario_version",rs.getInt(5));m.put("current_step",rs.getInt(6));return m;},id);if(r.isEmpty())throw notFound("Run not found");if(!Objects.equals(((Number)r.getFirst().get("started_by")).longValue(),userId(username))&&!isAdmin(username)&&!defenderAccess)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Run access denied");return r.getFirst();}
 public List<Map<String,Object>> events(long id){return jdbc.queryForList("SELECT id,sequence_number,event_type,source_asset_id,target_asset_id,outcome,severity,event_data,occurred_at FROM simulation_events WHERE run_id=? ORDER BY sequence_number",id);}
 public List<Map<String,Object>> alerts(long id){return jdbc.queryForList("SELECT id,rule_id,trigger_event_id,severity,status,evidence,acknowledged_by,acknowledged_at,created_at,updated_at FROM alerts WHERE run_id=? ORDER BY created_at DESC",id);}
 public List<Map<String,Object>> defenses(long id){return jdbc.queryForList("SELECT id,actor_id,target_asset_id,action_type,status,reason,result,created_at FROM defense_actions WHERE run_id=? ORDER BY id",id);}
 public List<Map<String,Object>> userAlerts(String username){return jdbc.queryForList("SELECT a.id,a.run_id,a.rule_id,a.trigger_event_id,a.severity,a.status,a.evidence,a.created_at FROM alerts a JOIN simulation_runs r ON r.id=a.run_id WHERE r.started_by=? ORDER BY a.created_at DESC LIMIT 100",userId(username));}
 public Map<String,Object> alert(long id,String username){var results=jdbc.query("SELECT a.id,a.run_id,a.rule_id,a.trigger_event_id,a.severity,a.status,a.evidence,a.acknowledged_by,a.acknowledged_at,a.created_at,a.updated_at,r.started_by FROM alerts a JOIN simulation_runs r ON r.id=a.run_id WHERE a.id=?",(rs,n)->{Map<String,Object> m=new LinkedHashMap<>();for(String c:List.of("id","run_id","rule_id","trigger_event_id","severity","status","evidence","acknowledged_by","acknowledged_at","created_at","updated_at","started_by"))m.put(c,rs.getObject(c));return m;},id);if(results.isEmpty())throw notFound("Alert not found");var alert=results.getFirst();if(!Objects.equals(((Number)alert.get("started_by")).longValue(),userId(username))&&!isAdmin(username))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Alert access denied");alert.remove("started_by");return alert;}
 public Map<String,Object> overview(String username){long uid=userId(username);return Map.of("runs",jdbc.queryForObject("SELECT COUNT(*) FROM simulation_runs WHERE started_by=?",Integer.class,uid),"events",jdbc.queryForObject("SELECT COUNT(*) FROM simulation_events e JOIN simulation_runs r ON r.id=e.run_id WHERE r.started_by=?",Integer.class,uid),"openAlerts",jdbc.queryForObject("SELECT COUNT(*) FROM alerts a JOIN simulation_runs r ON r.id=a.run_id WHERE r.started_by=? AND a.status='OPEN'",Integer.class,uid));}
 public Map<String,Object> metrics(long id){
   long eventCount=jdbc.queryForObject("SELECT COUNT(*) FROM simulation_events WHERE run_id=?",Long.class,id);
   long successful=jdbc.queryForObject("SELECT COUNT(*) FROM simulation_events WHERE run_id=? AND outcome='SUCCESS'",Long.class,id);
   long blocked=jdbc.queryForObject("SELECT COUNT(*) FROM simulation_events WHERE run_id=? AND outcome='BLOCKED'",Long.class,id);
   long reached=jdbc.queryForObject("SELECT COUNT(DISTINCT target_asset_id) FROM simulation_events WHERE run_id=? AND outcome='SUCCESS' AND target_asset_id IS NOT NULL",Long.class,id);
   var severity=jdbc.queryForList("SELECT severity,COUNT(*) AS count FROM alerts WHERE run_id=? GROUP BY severity",id);
   double risk=Math.min(100.0,Math.max(0.0,Math.round(Math.min(1.0,blocked/(double)Math.max(1,eventCount))*35.0+Math.min(1.0,reached/5.0)*35.0+Math.min(1.0,jdbc.queryForObject("SELECT COUNT(*) FROM alerts WHERE run_id=?",Integer.class,id)/5.0)*30.0)));
   Map<String,Object> result=new LinkedHashMap<>();result.put("runId",id);result.put("totalEvents",eventCount);result.put("successfulTransitions",successful);result.put("blockedTransitions",blocked);result.put("distinctAssetsReached",reached);result.put("alertsBySeverity",severity);result.put("detectionLatencyMillis",null);result.put("containmentLatencyMillis",null);result.put("finalModeledRiskScore",risk);result.put("riskModel","blocked 35%, reach 35%, alert volume 30%; prototype scale");return result;
 }
 private long userId(String u){var a=jdbc.query("SELECT id FROM users WHERE username=?",(rs,n)->rs.getLong(1),u);if(a.isEmpty())throw notFound("User not found");return a.getFirst();}
 private boolean isAdmin(String u){return Boolean.TRUE.equals(jdbc.queryForObject("SELECT role='ADMIN' FROM users WHERE username=?",Boolean.class,u));}
 private void audit(long user,String action,String entity,long entityId,Map<String,Object> details){jdbc.update("INSERT INTO audit_logs(actor_id,action,entity_type,entity_id,details) VALUES(?,?,?,?,CAST(? AS JSON))",user,action,entity,entityId,write(details));}
 private String write(Object value){try{return json.writeValueAsString(value);}catch(JsonProcessingException e){throw new IllegalStateException(e);}}
 @SuppressWarnings("unchecked") private Map<String,Object> jsonMap(Object value){try{if(value==null)return Map.of();if(value instanceof Map<?,?> map)return (Map<String,Object>)map;if(value instanceof byte[] bytes)return json.readValue(bytes,Map.class);return json.readValue(value.toString(),Map.class);}catch(Exception e){throw bad("Invalid step configuration");}}
 private Long num(Object o){return o==null?null:((Number)o).longValue();}
 private ResponseStatusException notFound(String m){return new ResponseStatusException(HttpStatus.NOT_FOUND,m);}
 private ResponseStatusException bad(String m){return new ResponseStatusException(HttpStatus.CONFLICT,m);}
}
