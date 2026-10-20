INSERT INTO detection_rules (name, description, event_type, rule_type, threshold, window_seconds, severity, enabled, version)
VALUES (
    'Account compromise detected',
    'Alert when a simulated account compromise succeeds.',
    'ACCOUNT_COMPROMISE',
    'ACCOUNT_COMPROMISE_DETECTED',
    1,
    300,
    'HIGH',
    TRUE,
    1
);
