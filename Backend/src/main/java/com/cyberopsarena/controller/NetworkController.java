package com.cyberopsarena.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;

@RestController @RequestMapping("/api/v1/network-templates")
public class NetworkController {
 private final JdbcTemplate jdbc;
 public NetworkController(JdbcTemplate jdbc){this.jdbc=jdbc;}
 public record TemplateRequest(@NotBlank String name,String description){}
 public record AssetRequest(@NotBlank String name,@NotBlank String assetType,@NotBlank String criticality,String initialState,Map<String,Object> attributes){}
 public record EdgeRequest(long sourceAssetId,long targetAssetId,@NotBlank String relationshipType){}
 @GetMapping public Object list(){return jdbc.queryForList("SELECT id,name,description,created_by,version,created_at,updated_at FROM network_templates ORDER BY id LIMIT 100");}
 @GetMapping("/{id}") public Object get(@PathVariable long id){var r=jdbc.query("SELECT id,name,description,created_by,version,created_at,updated_at FROM network_templates WHERE id=?",(rs,n)->{Map<String,Object> m=new LinkedHashMap<>();for(String c:List.of("id","name","description","created_by","version","created_at","updated_at"))m.put(c,rs.getObject(c));return m;},id);if(r.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Template not found");return r.getFirst();}
 @PostMapping @Transactional public Object create(@Valid @RequestBody TemplateRequest req,Authentication auth){long uid=jdbc.queryForObject("SELECT id FROM users WHERE username=?",Long.class,auth.getName());jdbc.update("INSERT INTO network_templates(name,description,created_by) VALUES(?,?,?)",req.name(),req.description(),uid);return get(jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class));}
 @GetMapping("/{id}/graph") public Object graph(@PathVariable long id){get(id);return Map.of("templateId",id,"assets",jdbc.queryForList("SELECT id,name,asset_type,criticality,initial_state FROM assets WHERE template_id=? ORDER BY id",id),"edges",jdbc.queryForList("SELECT id,source_asset_id,target_asset_id,relationship_type,enabled FROM network_edges WHERE template_id=? ORDER BY id",id));}
 @PostMapping("/{id}/assets") @Transactional public Object addAsset(@PathVariable long id,@Valid @RequestBody AssetRequest req){get(id);Set<String> types=Set.of("WORKSTATION","SERVER","DATABASE","USER_ACCOUNT","FIREWALL","SECURITY_MONITOR"),criticalities=Set.of("LOW","MEDIUM","HIGH","CRITICAL");String state=req.initialState()==null?"NORMAL":req.initialState();if(!types.contains(req.assetType())||!criticalities.contains(req.criticality())||!Set.of("NORMAL","COMPROMISED","DISABLED").contains(state))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unsupported asset type, criticality, or initial state");jdbc.update("INSERT INTO assets(template_id,name,asset_type,criticality,initial_state,attributes) VALUES(?,?,?,?,?,CAST(? AS JSON))",id,req.name(),req.assetType(),req.criticality(),state,toJson(req.attributes()==null?Map.of():req.attributes()));return jdbc.queryForMap("SELECT id,template_id,name,asset_type,criticality,initial_state,attributes FROM assets WHERE id=LAST_INSERT_ID()");}
 @PostMapping("/{id}/edges") @Transactional public Object addEdge(@PathVariable long id,@Valid @RequestBody EdgeRequest req){get(id);if(req.sourceAssetId()==req.targetAssetId())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Self edges are not permitted");Integer count=jdbc.queryForObject("SELECT COUNT(*) FROM assets WHERE template_id=? AND id IN (?,?)",Integer.class,id,req.sourceAssetId(),req.targetAssetId());if(count==null||count!=2)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Both assets must belong to this template");jdbc.update("INSERT INTO network_edges(template_id,source_asset_id,target_asset_id,relationship_type) VALUES(?,?,?,?)",id,req.sourceAssetId(),req.targetAssetId(),req.relationshipType());return jdbc.queryForMap("SELECT id,template_id,source_asset_id,target_asset_id,relationship_type,enabled,created_at FROM network_edges WHERE id=LAST_INSERT_ID()");}
 private String toJson(Object o){try{return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(o);}catch(Exception e){throw new IllegalArgumentException(e);}}
}
