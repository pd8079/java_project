package com.cyberopsarena.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import com.cyberopsarena.service.NetworkTemplateService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/v1/network-templates")
public class NetworkController {
 private final NetworkTemplateService templates;
 public NetworkController(NetworkTemplateService templates){this.templates=templates;}
 public record TemplateRequest(@NotBlank String name,String description){}
 public record AssetRequest(@NotBlank String name,@NotBlank String assetType,@NotBlank String criticality,String initialState,Map<String,Object> attributes){}
 public record EdgeRequest(long sourceAssetId,long targetAssetId,@NotBlank String relationshipType){}
 @GetMapping public Object list(){return templates.list();}
 @GetMapping("/{id}") public Object get(@PathVariable long id){return templates.get(id);}
 @PostMapping public Object create(@Valid @RequestBody TemplateRequest req,Authentication auth){return templates.create(req.name(),req.description(),auth.getName());}
 @GetMapping("/{id}/graph") public Object graph(@PathVariable long id){return templates.graph(id);}
 @PostMapping("/{id}/assets") public Object addAsset(@PathVariable long id,@Valid @RequestBody AssetRequest req){return templates.addAsset(id,req.name(),req.assetType(),req.criticality(),req.initialState(),req.attributes());}
 @PostMapping("/{id}/edges") public Object addEdge(@PathVariable long id,@Valid @RequestBody EdgeRequest req){return templates.addEdge(id,req.sourceAssetId(),req.targetAssetId(),req.relationshipType());}
}
