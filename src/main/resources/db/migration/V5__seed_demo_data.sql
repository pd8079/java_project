INSERT INTO network_templates (name, description, version) VALUES ('Demo Enterprise', 'Isolated three asset network for classroom exercises', 1);
INSERT INTO assets (template_id,name,asset_type,criticality,initial_state,attributes) VALUES
 (1,'analyst-workstation','WORKSTATION','MEDIUM','NORMAL',JSON_OBJECT()),
 (1,'employee-account','USER_ACCOUNT','HIGH','NORMAL',JSON_OBJECT()),
 (1,'finance-database','DATABASE','CRITICAL','NORMAL',JSON_OBJECT());
INSERT INTO network_edges (template_id,source_asset_id,target_asset_id,relationship_type) VALUES
 (1,1,2,'AUTHENTICATES_TO'),(1,2,3,'CAN_ACCESS');
INSERT INTO attack_scenarios (name,description,version,enabled) VALUES
 ('Account compromise and blocked movement','Compromise a virtual account then attempt movement toward a protected database.',1,TRUE),
 ('Suspicious login activity','Generate repeated failed simulated login activity to demonstrate detection.',1,TRUE);
INSERT INTO scenario_steps (scenario_id,step_order,step_type,source_asset_id,target_asset_id,preconditions,effects) VALUES
 (1,1,'ACCOUNT_COMPROMISE',1,2,JSON_OBJECT(),JSON_OBJECT('state','COMPROMISED')),
 (1,2,'LATERAL_MOVEMENT',2,3,JSON_OBJECT(),JSON_OBJECT()),
 (2,1,'SUSPICIOUS_LOGIN',1,2,JSON_OBJECT('attempts',5),JSON_OBJECT());
INSERT INTO detection_rules (name,description,event_type,rule_type,threshold,window_seconds,severity,enabled,version) VALUES
 ('Repeated failed logins','Detect repeated failed simulated logins in a time window.','SUSPICIOUS_LOGIN','REPEATED_FAILED_LOGIN',3,300,'HIGH',TRUE,1),
 ('Unexpected transition','Detect modeled movement between assets.','LATERAL_MOVEMENT','UNEXPECTED_TRANSITION',1,300,'MEDIUM',TRUE,1),
 ('High criticality access','Alert on simulated access to a critical asset.','LATERAL_MOVEMENT','HIGH_CRITICALITY_ACCESS',1,300,'CRITICAL',TRUE,1),
 ('Isolated asset attempt','Alert on transition attempts to an isolated asset.','BLOCKED_TRANSITION','ISOLATED_ASSET_ATTEMPT',1,300,'HIGH',TRUE,1);
