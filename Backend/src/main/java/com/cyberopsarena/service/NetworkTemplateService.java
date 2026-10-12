package com.cyberopsarena.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class NetworkTemplateService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public NetworkTemplateService(JdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public Object list() {
        return jdbc.queryForList("SELECT id,name,description,created_by,version,created_at,updated_at FROM network_templates ORDER BY id LIMIT 100");
    }

    public Map<String, Object> get(long id) {
        var rows = jdbc.query("SELECT id,name,description,created_by,version,created_at,updated_at FROM network_templates WHERE id=?", (rs, n) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            for (String column : List.of("id", "name", "description", "created_by", "version", "created_at", "updated_at")) row.put(column, rs.getObject(column));
            return row;
        }, id);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Template not found");
        return rows.getFirst();
    }

    @Transactional
    public Object create(String name, String description, String username) {
        long userId = jdbc.queryForObject("SELECT id FROM users WHERE username=?", Long.class, username);
        jdbc.update("INSERT INTO network_templates(name,description,created_by) VALUES(?,?,?)", name, description, userId);
        return get(jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class));
    }

    public Object graph(long id) {
        get(id);
        return Map.of("templateId", id,
                "assets", jdbc.queryForList("SELECT id,name,asset_type,criticality,initial_state FROM assets WHERE template_id=? ORDER BY id", id),
                "edges", jdbc.queryForList("SELECT id,source_asset_id,target_asset_id,relationship_type,enabled FROM network_edges WHERE template_id=? ORDER BY id", id));
    }

    @Transactional
    public Object addAsset(long id, String name, String assetType, String criticality, String initialState, Map<String, Object> attributes) {
        get(id);
        Set<String> types = Set.of("WORKSTATION", "SERVER", "DATABASE", "USER_ACCOUNT", "FIREWALL", "SECURITY_MONITOR");
        Set<String> criticalities = Set.of("LOW", "MEDIUM", "HIGH", "CRITICAL");
        String state = initialState == null ? "NORMAL" : initialState;
        if (!types.contains(assetType) || !criticalities.contains(criticality) || !Set.of("NORMAL", "COMPROMISED", "DISABLED").contains(state)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported asset type, criticality, or initial state");
        }
        jdbc.update("INSERT INTO assets(template_id,name,asset_type,criticality,initial_state,attributes) VALUES(?,?,?,?,?,CAST(? AS JSON))",
                id, name, assetType, criticality, state, toJson(attributes == null ? Map.of() : attributes));
        return jdbc.queryForMap("SELECT id,template_id,name,asset_type,criticality,initial_state,attributes FROM assets WHERE id=LAST_INSERT_ID()");
    }

    @Transactional
    public Object addEdge(long id, long sourceAssetId, long targetAssetId, String relationshipType) {
        get(id);
        if (sourceAssetId == targetAssetId) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Self edges are not permitted");
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM assets WHERE template_id=? AND id IN (?,?)", Integer.class, id, sourceAssetId, targetAssetId);
        if (count == null || count != 2) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Both assets must belong to this template");
        jdbc.update("INSERT INTO network_edges(template_id,source_asset_id,target_asset_id,relationship_type) VALUES(?,?,?,?)", id, sourceAssetId, targetAssetId, relationshipType);
        return jdbc.queryForMap("SELECT id,template_id,source_asset_id,target_asset_id,relationship_type,enabled,created_at FROM network_edges WHERE id=LAST_INSERT_ID()");
    }

    private String toJson(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Could not encode request data as JSON", exception);
        }
    }
}
