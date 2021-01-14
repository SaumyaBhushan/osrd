package fr.sncf.osrd.train;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fr.sncf.osrd.infra.topological.TopoEdge;
import fr.sncf.osrd.simulation.utils.Simulation;
import fr.sncf.osrd.simulation.utils.SimulationError;
import fr.sncf.osrd.simulation.utils.TimelineEvent;
import fr.sncf.osrd.speedcontroller.SpeedController;
import fr.sncf.osrd.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Objects;
import java.util.stream.Collectors;

public final class TrainState {
    static final Logger logger = LoggerFactory.getLogger(TrainState.class);

    // the time for which this state is relevant
    public final double time;

    // the current speed of the train
    public double speed;

    // what state the train is in: reached destination, rolling, emergency, ...
    public TrainStatus status;

    // the train this is the state of
    public final Train train;

    // this field MUST be kept private, as it is not the position of the train at the current simulation time,
    // but rather the position of the train at the last event. it's fine and expected, but SpeedControllers need
    // the simulated location
    public final TrainPositionTracker location;

    public final LinkedList<SpeedController> controllers;

    @Override
    @SuppressFBWarnings({"FE_FLOATING_POINT_EQUALITY"})
    public boolean equals(Object obj) {
        if (obj == null)
            return false;

        if (obj.getClass() != TrainState.class)
            return false;

        var otherState = (TrainState) obj;
        if (this.time != otherState.time)
            return false;
        if (this.speed != otherState.speed)
            return false;
        if (this.status != otherState.status)
            return false;
        if (!this.train.entityId.equals(otherState.train.entityId))
            return false;
        if (!this.location.equals(otherState.location))
            return false;
        return this.controllers.equals(otherState.controllers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(time, speed, status, train.entityId, location, controllers);
    }

    TrainState(
            double time,
            TrainPositionTracker location,
            double speed,
            TrainStatus status,
            LinkedList<SpeedController> controllers,
            Train train
    ) {
        this.time = time;
        this.location = location;
        this.speed = speed;
        this.status = status;
        this.controllers = controllers;
        this.train = train;
    }

    protected TrainState clone() {
        return new TrainState(
                time,
                location.clone(),
                speed,
                status,
                new LinkedList<>(controllers),
                train);
    }

    private TrainPhysicsIntegrator.PositionUpdate step(@SuppressWarnings("SameParameterValue") double timeStep) {
        // TODO: find out the actual max braking / acceleration force

        var rollingStock = train.rollingStock;
        var simulator = TrainPhysicsIntegrator.make(
                timeStep,
                rollingStock,
                speed,
                location.maxTrainGrade());

        Action action = getAction(location, simulator);
        logger.trace("train took action {}", action);

        assert action != null;

        // compute and limit the traction force
        var maxTraction = rollingStock.getMaxEffort(speed);
        var traction = action.tractionForce();
        if (traction > maxTraction)
            traction = maxTraction;

        // compute and limit the braking force
        var brakingForce = action.brakingForce();

        var update = simulator.computeUpdate(traction, brakingForce);
        // TODO: handle emergency braking

        logger.trace("speed changed from {} to {}", speed, update.speed);
        speed = update.speed;
        location.updatePosition(update.positionDelta);
        return update;
    }

    private Train.LocationChange computeSpeedCurve(
            Simulation sim,
            double goalTrackPosition
    ) throws SimulationError {
        var nextState = this.clone();
        var positionUpdates = new ArrayList<TrainPhysicsIntegrator.PositionUpdate>();

        for (int i = 0; nextState.location.getHeadPathPosition() < goalTrackPosition; i++) {
            if (i >= 10000)
                throw new SimulationError("train physics numerical integration doesn't seem to stop");

            if (nextState.location.hasReachedGoal()) {
                nextState.status = TrainStatus.REACHED_DESTINATION;
                break;
            }

            var update = nextState.step(1.0);
            positionUpdates.add(update);
        }

        return new Train.LocationChange(sim, train, nextState, positionUpdates);
    }

    private Action getAction(TrainPositionTracker location, TrainPhysicsIntegrator trainPhysics) {
        switch (status) {
            case STARTING_UP:
            case STOP:
            case ROLLING:
                return updateRolling(location, trainPhysics);
            case EMERGENCY_BRAKING:
            case REACHED_DESTINATION:
                return null;
            default:
                throw new RuntimeException("Invalid train state");
        }
    }

    private Action updateRolling(TrainPositionTracker position, TrainPhysicsIntegrator trainPhysics) {
        var actions = controllers.stream()
                .map(sp -> new Pair<>(sp, sp.getAction(this, trainPhysics)))
                .collect(Collectors.toList());

        var action = actions.stream()
                .map(pair -> pair.second)
                .filter(curAction -> curAction.type != Action.ActionType.NO_ACTION)
                .min(Action::compareTo);
        assert action.isPresent();

        var toDelete = actions.stream()
                .filter(pair -> pair.second.deleteController)
                .map(pair -> pair.first)
                .collect(Collectors.toList());
        controllers.removeAll(toDelete);

        return action.get();
    }


    @SuppressWarnings("UnnecessaryLocalVariable")
    TimelineEvent<Train.LocationChange> simulateUntilEvent(Simulation sim) throws SimulationError {
        // 1) find the next event position

        // look for objects in the range [train_position, +inf)
        // TODO: optimize, we don't need to iterate on all the path
        var nextTrackObjectVisibilityChange = location
                .streamPointAttrForward(Double.POSITIVE_INFINITY, TopoEdge::getVisibleTrackObjects)
                .map(pointValue -> {
                    // the position of track object relative to path of the train
                    // (the distance to the train's starting point)
                    var pathObjectPosition = pointValue.position;
                    var sightDistance = Math.min(train.driverSightDistance, pointValue.value.getSightDistance());
                    // return the path position at which the object becomes visible
                    return pathObjectPosition - sightDistance;
                })
                .min(Double::compareTo)
                // TODO: that's pretty meh
                .orElse(Double.POSITIVE_INFINITY);

        // for now, we only handle visible track objects
        var nextEventTrackPosition = nextTrackObjectVisibilityChange;

        // 2) simulate up to nextEventTrackPosition
        var simulationResult = computeSpeedCurve(sim, nextEventTrackPosition);
        var simulationElapsedTime = simulationResult.positionUpdates.stream()
                .map(update -> update.timeDelta)
                .reduce(Double::sum)
                .orElse(0.0);
        var simulationTime = sim.getTime() + simulationElapsedTime;

        // 3) create an event with simulation data up to this point
        return train.event(sim, simulationTime, simulationResult);
    }

    @SuppressFBWarnings({"UPM_UNCALLED_PRIVATE_METHOD"})
    private double getMaxAcceleration() {
        if (status == TrainStatus.STARTING_UP)
            return train.rollingStock.startUpAcceleration;
        return train.rollingStock.comfortAcceleration;
    }
}
