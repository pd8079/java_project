CREATE TABLE users (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, username VARCHAR(80) NOT NULL UNIQUE, email VARCHAR(254) NOT NULL UNIQUE,
 password_hash VARCHAR(100) NOT NULL, role VARCHAR(20) NOT NULL, enabled BOOLEAN NOT NULL DEFAULT TRUE,
 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
 CONSTRAINT chk_user_role CHECK (role IN ('ADMIN','ANALYST','DEFENDER'))
) ENGINE=InnoDB;
CREATE TABLE network_templates (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(120) NOT NULL, description TEXT, created_by BIGINT,
 version INT NOT NULL DEFAULT 1, created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
 CONSTRAINT fk_template_creator FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB;
CREATE TABLE assets (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, template_id BIGINT NOT NULL, name VARCHAR(120) NOT NULL,
 asset_type VARCHAR(30) NOT NULL, criticality VARCHAR(20) NOT NULL DEFAULT 'MEDIUM', initial_state VARCHAR(30) NOT NULL DEFAULT 'NORMAL',
 attributes JSON, created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
 CONSTRAINT fk_asset_template FOREIGN KEY (template_id) REFERENCES network_templates(id) ON DELETE RESTRICT,
 CONSTRAINT chk_asset_type CHECK (asset_type IN ('WORKSTATION','SERVER','DATABASE','USER_ACCOUNT','FIREWALL','SECURITY_MONITOR')),
 CONSTRAINT chk_asset_criticality CHECK (criticality IN ('LOW','MEDIUM','HIGH','CRITICAL')),
 CONSTRAINT chk_asset_initial_state CHECK (initial_state IN ('NORMAL','COMPROMISED','DISABLED')),
 INDEX ix_asset_template (template_id)
) ENGINE=InnoDB;
CREATE TABLE network_edges (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, template_id BIGINT NOT NULL, source_asset_id BIGINT NOT NULL, target_asset_id BIGINT NOT NULL,
 relationship_type VARCHAR(40) NOT NULL, enabled BOOLEAN NOT NULL DEFAULT TRUE, created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 CONSTRAINT fk_edge_template FOREIGN KEY (template_id) REFERENCES network_templates(id) ON DELETE RESTRICT,
 CONSTRAINT fk_edge_source FOREIGN KEY (source_asset_id) REFERENCES assets(id) ON DELETE RESTRICT,
 CONSTRAINT fk_edge_target FOREIGN KEY (target_asset_id) REFERENCES assets(id) ON DELETE RESTRICT,
 CONSTRAINT chk_edge_distinct CHECK (source_asset_id <> target_asset_id),
 UNIQUE KEY uq_edge (template_id, source_asset_id, target_asset_id, relationship_type), INDEX ix_edge_source (source_asset_id), INDEX ix_edge_target (target_asset_id)
) ENGINE=InnoDB;
CREATE TABLE attack_scenarios (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(120) NOT NULL, description TEXT, version INT NOT NULL DEFAULT 1,
 enabled BOOLEAN NOT NULL DEFAULT TRUE, created_by BIGINT, created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 CONSTRAINT fk_scenario_creator FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL, INDEX ix_scenario_enabled (enabled)
) ENGINE=InnoDB;
CREATE TABLE scenario_steps (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, scenario_id BIGINT NOT NULL, step_order INT NOT NULL, step_type VARCHAR(50) NOT NULL,
 source_asset_id BIGINT, target_asset_id BIGINT, preconditions JSON, effects JSON, created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 CONSTRAINT fk_step_scenario FOREIGN KEY (scenario_id) REFERENCES attack_scenarios(id) ON DELETE RESTRICT,
 CONSTRAINT fk_step_source FOREIGN KEY (source_asset_id) REFERENCES assets(id) ON DELETE RESTRICT,
 CONSTRAINT fk_step_target FOREIGN KEY (target_asset_id) REFERENCES assets(id) ON DELETE RESTRICT,
 UNIQUE KEY uq_scenario_step (scenario_id, step_order), INDEX ix_step_scenario (scenario_id)
) ENGINE=InnoDB;
