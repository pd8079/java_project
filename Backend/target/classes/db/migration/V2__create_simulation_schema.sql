CREATE TABLE simulation_runs (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, scenario_id BIGINT NOT NULL, started_by BIGINT NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'CREATED',
 scenario_version INT NOT NULL, initial_snapshot JSON NOT NULL, current_step INT NOT NULL DEFAULT 0, version BIGINT NOT NULL DEFAULT 0,
 started_at TIMESTAMP(6) NULL, finished_at TIMESTAMP(6) NULL, created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
 CONSTRAINT fk_run_scenario FOREIGN KEY (scenario_id) REFERENCES attack_scenarios(id) ON DELETE RESTRICT,
 CONSTRAINT fk_run_owner FOREIGN KEY (started_by) REFERENCES users(id) ON DELETE RESTRICT,
 CONSTRAINT chk_run_status CHECK (status IN ('CREATED','RUNNING','PAUSED','COMPLETED','STOPPED','FAILED')),
 INDEX ix_run_owner_status (started_by,status), INDEX ix_run_created (created_at)
) ENGINE=InnoDB;
CREATE TABLE simulation_run_assets (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, run_id BIGINT NOT NULL, asset_id BIGINT NOT NULL, current_state VARCHAR(30) NOT NULL,
 isolated BOOLEAN NOT NULL DEFAULT FALSE, disabled BOOLEAN NOT NULL DEFAULT FALSE, state_data JSON,
 updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
 CONSTRAINT fk_run_asset_run FOREIGN KEY (run_id) REFERENCES simulation_runs(id) ON DELETE RESTRICT,
 CONSTRAINT fk_run_asset_asset FOREIGN KEY (asset_id) REFERENCES assets(id) ON DELETE RESTRICT,
 UNIQUE KEY uq_run_asset (run_id,asset_id)
) ENGINE=InnoDB;
CREATE TABLE simulation_run_edges (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, run_id BIGINT NOT NULL, network_edge_id BIGINT NOT NULL, enabled BOOLEAN NOT NULL DEFAULT TRUE, blocked BOOLEAN NOT NULL DEFAULT FALSE,
 updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
 CONSTRAINT fk_run_edge_run FOREIGN KEY (run_id) REFERENCES simulation_runs(id) ON DELETE RESTRICT,
 CONSTRAINT fk_run_edge_edge FOREIGN KEY (network_edge_id) REFERENCES network_edges(id) ON DELETE RESTRICT,
 UNIQUE KEY uq_run_edge (run_id,network_edge_id)
) ENGINE=InnoDB;
CREATE TABLE simulation_events (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, run_id BIGINT NOT NULL, sequence_number BIGINT NOT NULL, event_type VARCHAR(50) NOT NULL,
 source_asset_id BIGINT NULL, target_asset_id BIGINT NULL, outcome VARCHAR(30) NOT NULL, severity VARCHAR(20) NOT NULL,
 event_data JSON, occurred_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 CONSTRAINT fk_event_run FOREIGN KEY (run_id) REFERENCES simulation_runs(id) ON DELETE RESTRICT,
 CONSTRAINT fk_event_source FOREIGN KEY (source_asset_id) REFERENCES assets(id) ON DELETE RESTRICT,
 CONSTRAINT fk_event_target FOREIGN KEY (target_asset_id) REFERENCES assets(id) ON DELETE RESTRICT,
 UNIQUE KEY uq_event_sequence (run_id,sequence_number), INDEX ix_event_chronology (run_id,sequence_number), INDEX ix_event_type (run_id,event_type), INDEX ix_event_severity (run_id,severity)
) ENGINE=InnoDB;
