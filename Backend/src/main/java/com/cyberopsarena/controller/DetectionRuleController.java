package com.cyberopsarena.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@RestController @RequestMapping("/api/v1/detection-rules")
public class DetectionRuleController {
 private final JdbcTemplate jdbc;
 public DetectionRuleController(JdbcTemplate jdbc){this.jdbc=jdbc;}
 public record RuleRequest(@NotBlank String name,String description,@NotBlank String eventType,@NotBlank String ruleType,@Positive int threshold,@Positive int windowSeconds,@NotBlank String severity,Boolean enabled){}
 @GetMapping public Object list(){return jdbc.queryForList("SELECT id,name,description,event_type,rule_type,threshold,window_seconds,severity,enabled,version,created_at,updated_at FROM detection_rules ORDER BY id");}
 @GetMapping("/{id}") public Object get(@PathVariable long id){return jdbc.queryForMap("SELECT id,name,description,event_type,rule_type,threshold,window_seconds,severity,enabled,version,created_at,updated_at FROM detection_rules WHERE id=?",id);}
 @PostMapping @Transactional @PreAuthorize("hasAnyRole('ADMIN','ANALYST')") public Object create(@Valid @RequestBody RuleRequest r){validate(r);jdbc.update("INSERT INTO detection_rules(name,description,event_type,rule_type,threshold,window_seconds,severity,enabled) VALUES(?,?,?,?,?,?,?,?)",r.name(),r.description(),r.eventType(),r.ruleType(),r.threshold(),r.windowSeconds(),r.severity(),r.enabled()==null||r.enabled());return get(jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class));}
 @PatchMapping("/{id}") @PreAuthorize("hasAnyRole('ADMIN','ANALYST')") public Object patch(@PathVariable long id,@Valid @RequestBody RuleRequest r){get(id);validate(r);jdbc.update("UPDATE detection_rules SET name=?,description=?,event_type=?,rule_type=?,threshold=?,window_seconds=?,severity=?,enabled=?,version=version+1 WHERE id=?",r.name(),r.description(),r.eventType(),r.ruleType(),r.threshold(),r.windowSeconds(),r.severity(),r.enabled()==null||r.enabled(),id);return get(id);}
 private void validate(RuleRequest r){if(!Set.of("REPEATED_FAILED_LOGIN","UNEXPECTED_TRANSITION","ISOLATED_ASSET_ATTEMPT","HIGH_CRITICALITY_ACCESS").contains(r.ruleType()))throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Unsupported detection rule type");if(!Set.of("LOW","MEDIUM","HIGH","CRITICAL").contains(r.severity()))throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Unsupported severity");}
}
