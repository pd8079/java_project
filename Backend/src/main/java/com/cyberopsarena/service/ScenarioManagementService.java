package com.cyberopsarena.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Set;

@Service
public class ScenarioManagementService {
    private final JdbcTemplate jdbc;
    private final SimulationService simulations;
    private final ObjectMapper json;

    public ScenarioManagementService(JdbcTemplate jdbc, SimulationService simulations, ObjectMapper json) {
        this.jdbc = jdbc;
        this.simulations = simulations;
        this.json = json;
    }

    public Object list() {
        return simulations.scenarios();
    }

    public Object get(long id) {
        return simulations.scenario(id);
    }

    @Transactional
    public Object create(String name, String description, String username) {
        long actor = jdbc.queryForObject("SELECT id FROM users WHERE username=?", Long.class, username);
        jdbc.update("INSERT INTO attack_scenarios(name,description,version,enabled,created_by) VALUES(?,?,1,TRUE,?)", name, description, actor);
        long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        audit(actor, "SCENARIO_CREATED", id);
        return get(id);
    }

    @Transactional
    public Object addStep(long id, int stepOrder, String stepType, Long sourceAssetId, Long targetAssetId,
                          Map<String, Object> preconditions, Map<String, Object> effects, String username) {
        var scenario = jdbc.query("SELECT id FROM attack_scenarios WHERE id=? AND enabled=TRUE FOR UPDATE", (row, n) -> row.getLong(1), id);
        if (scenario.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Scenario not found");
        if (jdbc.queryForObject("SELECT COUNT(*) FROM simulation_runs WHERE scenario_id=?", Integer.class, id) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Scenario is immutable after a run has been created");
        }
        if (!Set.of("ACCOUNT_COMPROMISE", "LATERAL_MOVEMENT", "SUSPICIOUS_LOGIN").contains(stepType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported scenario step type");
        }

        Map<String, Object> safePreconditions = preconditions == null ? Map.of() : preconditions;
        Map<String, Object> safeEffects = effects == null ? Map.of() : effects;
        if (!safeEffects.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This step type does not accept caller-supplied effects");
        if ("SUSPICIOUS_LOGIN".equals(stepType)) {
            Object attemptsValue = safePreconditions.getOrDefault("attempts", 5);
            if (!Set.of("attempts").containsAll(safePreconditions.keySet())
                    || !(attemptsValue instanceof Number attempts) || attempts.intValue() < 1 || attempts.intValue() > 100) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Suspicious login attempts must be an integer from 1 to 100");
            }
        } else if (!safePreconditions.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This step type does not accept preconditions");
        }
        if (sourceAssetId == null || targetAssetId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Source and target assets are required");

        var assets = jdbc.query("SELECT id,template_id FROM assets WHERE id IN (?,?)", (row, n) -> Map.of(row.getLong(1), row.getLong(2)), sourceAssetId, targetAssetId);
        if (assets.size() != 2) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Scenario assets must exist");
        Map<Long, Long> templateByAsset = new java.util.HashMap<>();
        assets.forEach(templateByAsset::putAll);
        Long sourceTemplate = templateByAsset.get(sourceAssetId);
        Long targetTemplate = templateByAsset.get(targetAssetId);
        if (sourceTemplate == null || targetTemplate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Scenario assets must exist");
        }
        if (!sourceTemplate.equals(targetTemplate)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Scenario assets must belong to the same template");
        Integer existing = jdbc.queryForObject("SELECT COUNT(*) FROM scenario_steps ss JOIN assets a ON a.id=ss.source_asset_id WHERE ss.scenario_id=? AND a.template_id<>?", Integer.class, id, sourceTemplate);
        if (existing != null && existing > 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "All steps in a scenario must use one template");

        jdbc.update("INSERT INTO scenario_steps(scenario_id,step_order,step_type,source_asset_id,target_asset_id,preconditions,effects) VALUES(?,?,?,?,?,CAST(? AS JSON),CAST(? AS JSON))",
                id, stepOrder, stepType, sourceAssetId, targetAssetId, toJson(safePreconditions), toJson(safeEffects));
        jdbc.update("UPDATE attack_scenarios SET version=version+1 WHERE id=?", id);
        long actor = jdbc.queryForObject("SELECT id FROM users WHERE username=?", Long.class, username);
        audit(actor, "SCENARIO_STEP_CREATED", id);
        return get(id);
    }

    private void audit(long actor, String action, long id) {
        jdbc.update("INSERT INTO audit_logs(actor_id,action,entity_type,entity_id,details) VALUES(?,?,'attack_scenarios',?,JSON_OBJECT())", actor, action, id);
    }

    private String toJson(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Could not encode scenario data as JSON", exception);
        }
    }
}
