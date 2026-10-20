package com.cyberopsarena.controller;

import com.cyberopsarena.service.SimulationService;
import com.cyberopsarena.service.BackgroundSimulationRunner;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController @RequestMapping("/api/v1")
public class SimulationController {
 private final SimulationService service;
 private final BackgroundSimulationRunner backgroundRunner;
 public SimulationController(SimulationService service,BackgroundSimulationRunner backgroundRunner){this.service=service;this.backgroundRunner=backgroundRunner;}
 public record CreateRun(@NotNull Long scenarioId){}
 public record DefenseRequest(@NotBlank String actionType,@NotBlank String reason,Long targetAssetId,Long edgeId,Long alertId){}
 @PostMapping("/simulation-runs") public Object create(@Valid @RequestBody CreateRun body,Authentication auth){return service.createRun(body.scenarioId(),auth.getName());}
 @GetMapping("/simulation-runs") public Object runs(Authentication auth){return service.runs(auth.getName());}
 @GetMapping("/simulation-runs/{id}") public Object run(@PathVariable long id,Authentication auth){return service.run(id,auth.getName());}
 @PostMapping("/simulation-runs/{id}/start") public Object start(@PathVariable long id,Authentication auth){return service.start(id,auth.getName());}
 @PostMapping("/simulation-runs/{id}/execute") public ResponseEntity<Map<String,Object>> execute(@PathVariable long id,Authentication auth){backgroundRunner.execute(id,auth.getName());return ResponseEntity.accepted().body(Map.of("runId",id,"status","SCHEDULED"));}
 @PostMapping("/simulation-runs/{id}/step") public Object step(@PathVariable long id,Authentication auth){return service.step(id,auth.getName());}
 @PostMapping("/simulation-runs/{id}/pause") public Object pause(@PathVariable long id,Authentication auth){return service.transition(id,auth.getName(),"PAUSED");}
 @PostMapping("/simulation-runs/{id}/stop") public Object stop(@PathVariable long id,Authentication auth){return service.transition(id,auth.getName(),"STOPPED");}
 @GetMapping("/simulation-runs/{id}/events") public Object events(@PathVariable long id,Authentication auth){service.run(id,auth.getName());return service.events(id);}
 @GetMapping("/simulation-runs/{id}/assets") public Object assets(@PathVariable long id,Authentication auth){service.run(id,auth.getName());return service.runAssets(id);}
 @GetMapping("/simulation-runs/{id}/edges") public Object edges(@PathVariable long id,Authentication auth){service.run(id,auth.getName());return service.runEdges(id);}
 @GetMapping("/simulation-runs/{id}/alerts") public Object alerts(@PathVariable long id,Authentication auth){service.run(id,auth.getName());return service.alerts(id);}
 @PostMapping("/simulation-runs/{id}/defenses") public Object defend(@PathVariable long id,@Valid @RequestBody DefenseRequest req,Authentication auth){return service.defend(id,auth.getName(),req);}
 @GetMapping("/simulation-runs/{id}/defenses") public Object defenses(@PathVariable long id,Authentication auth){service.run(id,auth.getName());return service.defenses(id);}
 @GetMapping("/simulation-runs/{id}/metrics") public Object metrics(@PathVariable long id,Authentication auth){service.run(id,auth.getName());return service.metrics(id);}
 @GetMapping("/simulation-runs/compare") public Object compare(@RequestParam long firstRunId,@RequestParam long secondRunId,Authentication auth){service.run(firstRunId,auth.getName());service.run(secondRunId,auth.getName());return Map.of("first",service.metrics(firstRunId),"second",service.metrics(secondRunId));}
 @GetMapping("/alerts") public Object alerts(Authentication auth){return service.userAlerts(auth.getName());}
 @GetMapping("/alerts/{id}") public Object alert(@PathVariable long id,Authentication auth){return service.alert(id,auth.getName());}
 @PostMapping("/alerts/{id}/acknowledge") public Object acknowledge(@PathVariable long id,Authentication auth){return service.alertStatus(id,auth.getName(),"ACKNOWLEDGED");}
 @PostMapping("/alerts/{id}/resolve") public Object resolve(@PathVariable long id,Authentication auth){return service.alertStatus(id,auth.getName(),"RESOLVED");}
 @GetMapping("/analytics/overview") public Object overview(Authentication auth){return service.overview(auth.getName());}
}
