CREATE TABLE audit_logs (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, actor_id BIGINT NULL, action VARCHAR(100) NOT NULL, entity_type VARCHAR(80) NOT NULL,
 entity_id BIGINT NULL, details JSON, created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 CONSTRAINT fk_audit_actor FOREIGN KEY (actor_id) REFERENCES users(id) ON DELETE SET NULL,
 INDEX ix_audit_actor_time (actor_id,created_at), INDEX ix_audit_entity (entity_type,entity_id), INDEX ix_audit_time (created_at)
) ENGINE=InnoDB;
