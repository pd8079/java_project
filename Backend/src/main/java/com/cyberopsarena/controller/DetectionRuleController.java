package com.cyberopsarena.controller;

import com.cyberopsarena.service.DetectionRuleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/detection-rules")
public class DetectionRuleController {
 private final DetectionRuleService rules;
 public DetectionRuleController(DetectionRuleService rules){this.rules=rules;}
 public record RuleRequest(@NotBlank String name,String description,@NotBlank String eventType,@NotBlank String ruleType,@Positive int threshold,@Positive int windowSeconds,@NotBlank String severity,Boolean enabled){}
 @GetMapping public Object list(){return rules.list();}
 @GetMapping("/{id}") public Object get(@PathVariable long id){return rules.get(id);}
 @PostMapping @PreAuthorize("hasAnyRole('ADMIN','ANALYST')") public Object create(@Valid @RequestBody RuleRequest r){return rules.create(command(r));}
 @PatchMapping("/{id}") @PreAuthorize("hasAnyRole('ADMIN','ANALYST')") public Object patch(@PathVariable long id,@Valid @RequestBody RuleRequest r){return rules.patch(id,command(r));}
 private DetectionRuleService.RuleCommand command(RuleRequest r){return new DetectionRuleService.RuleCommand(r.name(),r.description(),r.eventType(),r.ruleType(),r.threshold(),r.windowSeconds(),r.severity(),r.enabled());}
}
