package com.cyberopsarena.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Executes a simulation run on a named worker thread while clients poll the run resource. */
@Service
public class BackgroundSimulationRunner {
    private static final Logger log = LoggerFactory.getLogger(BackgroundSimulationRunner.class);
    private final SimulationService simulations;
    private final ConcurrentMap<Long, Thread> workers = new ConcurrentHashMap<>();

    public BackgroundSimulationRunner(SimulationService simulations) {
        this.simulations = simulations;
    }

    public void execute(long runId, String username) {
        Map<String, Object> run = simulations.run(runId, username);
        if (!"CREATED".equals(run.get("status"))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only a newly created run can execute in the background");
        }

        Thread worker = new Thread(new RunTask(runId, username), "simulation-run-" + runId);
        if (workers.putIfAbsent(runId, worker) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This run is already executing");
        }
        try {
            worker.start();
        } catch (RuntimeException exception) {
            workers.remove(runId, worker);
            throw exception;
        }
    }

    private final class RunTask implements Runnable {
        private final long runId;
        private final String username;

        private RunTask(long runId, String username) {
            this.runId = runId;
            this.username = username;
        }

        @Override
        public void run() {
            try {
                simulations.start(runId, username);
                while ("RUNNING".equals(simulations.run(runId, username).get("status"))) {
                    simulations.step(runId, username);
                }
            } catch (RuntimeException exception) {
                try {
                    simulations.fail(runId, username);
                } catch (RuntimeException failureUpdate) {
                    exception.addSuppressed(failureUpdate);
                }
                log.error("Background execution failed for simulation run {}", runId, exception);
            } finally {
                workers.remove(runId, Thread.currentThread());
            }
        }
    }
}
