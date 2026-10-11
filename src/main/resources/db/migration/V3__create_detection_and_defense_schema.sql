CREATE TABLE detection_rules (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(120) NOT NULL, description TEXT, event_type VARCHAR(50) NOT NULL,
 rule_type VARCHAR(50) NOT NULL, threshold INT NOT NULL DEFAULT 1, window_seconds INT NOT NULL DEFAULT 300,
 severity VARCHAR(20) NOT NULL, enabled BOOLEAN NOT NULL DEFAULT TRUE, version INT NOT NULL DEFAULT 1,
 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
 CONSTRAINT chk_rule_threshold CHECK (threshold > 0), INDEX ix_rule_enabled_type (enabled,event_type)
) ENGINE=InnoDB;
CREATE TABLE alerts (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, run_id BIGINT NOT NULL, rule_id BIGINT NOT NULL, trigger_event_id BIGINT NOT NULL,
 severity VARCHAR(20) NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'OPEN', evidence JSON NOT NULL,
 acknowledged_by BIGINT NULL, acknowledged_at TIMESTAMP(6) NULL, created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
 CONSTRAINT fk_alert_run FOREIGN KEY (run_id) REFERENCES simulation_runs(id) ON DELETE RESTRICT,
 CONSTRAINT fk_alert_rule FOREIGN KEY (rule_id) REFERENCES detection_rules(id) ON DELETE RESTRICT,
 CONSTRAINT fk_alert_event FOREIGN KEY (trigger_event_id) REFERENCES simulation_events(id) ON DELETE RESTRICT,
 CONSTRAINT fk_alert_actor FOREIGN KEY (acknowledged_by) REFERENCES users(id) ON DELETE SET NULL,
 CONSTRAINT chk_alert_status CHECK (status IN ('OPEN','ACKNOWLEDGED','RESOLVED')),
 UNIQUE KEY uq_alert_event_rule (rule_id,trigger_event_id), INDEX ix_alert_run_status (run_id,status)
) ENGINE=InnoDB;
CREATE TABLE defense_actions (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, run_id BIGINT NOT NULL, actor_id BIGINT NOT NULL, target_asset_id BIGINT NULL,
 action_type VARCHAR(40) NOT NULL, status VARCHAR(20) NOT NULL, reason VARCHAR(500) NOT NULL, result JSON,
 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 CONSTRAINT fk_defense_run FOREIGN KEY (run_id) REFERENCES simulation_runs(id) ON DELETE RESTRICT,
 CONSTRAINT fk_defense_actor FOREIGN KEY (actor_id) REFERENCES users(id) ON DELETE RESTRICT,
 CONSTRAINT fk_defense_asset FOREIGN KEY (target_asset_id) REFERENCES assets(id) ON DELETE RESTRICT, INDEX ix_defense_run (run_id,created_at)
) ENGINE=InnoDB;
