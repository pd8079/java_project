package com.cyberopsarena.controller;

import com.cyberopsarena.service.ScenarioManagementService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/v1/scenarios")
public class ScenarioController {
 private final ScenarioManagementService scenarios;
 public ScenarioController(ScenarioManagementService scenarios){this.scenarios=scenarios;}
 public record ScenarioRequest(@NotBlank String name,String description){}
 public record StepRequest(@Positive int stepOrder,@NotBlank String stepType,Long sourceAssetId,Long targetAssetId,Map<String,Object> preconditions,Map<String,Object> effects){}
 @GetMapping public Object list(){return scenarios.list();}
 @GetMapping("/{id}") public Object get(@PathVariable long id){return scenarios.get(id);}
 @PostMapping @PreAuthorize("hasAnyRole('ADMIN','ANALYST')") public Object create(@Valid @RequestBody ScenarioRequest req,Authentication auth){return scenarios.create(req.name(),req.description(),auth.getName());}
 @PostMapping("/{id}/steps") @PreAuthorize("hasAnyRole('ADMIN','ANALYST')") public Object addStep(@PathVariable long id,@Valid @RequestBody StepRequest req,Authentication auth){return scenarios.addStep(id,req.stepOrder(),req.stepType(),req.sourceAssetId(),req.targetAssetId(),req.preconditions(),req.effects(),auth.getName());}
}
