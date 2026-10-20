package com.cyberopsarena.service;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Set;

@Service
public class DetectionRuleService {
    public record RuleCommand(String name, String description, String eventType, String ruleType,
                              int threshold, int windowSeconds, String severity, Boolean enabled) {}

    private final JdbcTemplate jdbc;

    public DetectionRuleService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Object list() {
        return jdbc.queryForList("SELECT id,name,description,event_type,rule_type,threshold,window_seconds,severity,enabled,version,created_at,updated_at FROM detection_rules ORDER BY id");
    }

    public Map<String, Object> get(long id) {
        return jdbc.queryForMap("SELECT id,name,description,event_type,rule_type,threshold,window_seconds,severity,enabled,version,created_at,updated_at FROM detection_rules WHERE id=?", id);
    }

    @Transactional
    public Map<String, Object> create(RuleCommand rule) {
        validate(rule);
        jdbc.update("INSERT INTO detection_rules(name,description,event_type,rule_type,threshold,window_seconds,severity,enabled) VALUES(?,?,?,?,?,?,?,?)",
                rule.name(), rule.description(), rule.eventType(), rule.ruleType(), rule.threshold(), rule.windowSeconds(),
                rule.severity(), rule.enabled() == null || rule.enabled());
        return get(jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class));
    }

    @Transactional
    public Map<String, Object> patch(long id, RuleCommand rule) {
        get(id);
        validate(rule);
        jdbc.update("UPDATE detection_rules SET name=?,description=?,event_type=?,rule_type=?,threshold=?,window_seconds=?,severity=?,enabled=?,version=version+1 WHERE id=?",
                rule.name(), rule.description(), rule.eventType(), rule.ruleType(), rule.threshold(), rule.windowSeconds(),
                rule.severity(), rule.enabled() == null || rule.enabled(), id);
        return get(id);
    }

    private void validate(RuleCommand rule) {
        if (!Set.of("REPEATED_FAILED_LOGIN", "UNEXPECTED_TRANSITION", "ISOLATED_ASSET_ATTEMPT", "HIGH_CRITICALITY_ACCESS", "ACCOUNT_COMPROMISE_DETECTED").contains(rule.ruleType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported detection rule type");
        }
        if (!Set.of("LOW", "MEDIUM", "HIGH", "CRITICAL").contains(rule.severity())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported severity");
        }
    }
}
