package fr.sncf.osrd.envelope_sim.overlays;

import fr.sncf.osrd.envelope.EnvelopePartMeta;
import fr.sncf.osrd.envelope.InteractiveEnvelopePartConsumer;
import fr.sncf.osrd.envelope_sim.*;

public class EnvelopeAcceleration {
    /** Accelerate, storing the resulting steps into consumer */
    public static void accelerate(
            PhysicsRollingStock rollingStock,
            PhysicsPath path,
            double timeStep,
            double startPosition,
            double startSpeed,
            InteractiveEnvelopePartConsumer consumer,
            double directionSign
    ) {
        consumer.initEnvelopePart(startPosition, startSpeed, directionSign);
        consumer.setEnvelopePartMeta(new EnvelopePartMeta(EnvelopeProfile.class, EnvelopeProfile.ACCELERATING));
        double position = startPosition;
        double speed = startSpeed;
        while (true) {
            var step = TrainPhysicsIntegrator.step(
                    rollingStock, path, timeStep, position, speed, Action.ACCELERATE, directionSign
            );
            position += step.positionDelta;
            speed = step.endSpeed;
            if (!consumer.addStep(position, speed, step.timeDelta))
                break;
        }
    }
}
