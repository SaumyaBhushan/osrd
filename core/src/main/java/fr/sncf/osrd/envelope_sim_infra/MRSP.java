package fr.sncf.osrd.envelope_sim_infra;

import fr.sncf.osrd.envelope.Envelope;
import fr.sncf.osrd.envelope.EnvelopeAttr;
import fr.sncf.osrd.envelope.MRSPEnvelopeBuilder;
import fr.sncf.osrd.envelope.part.EnvelopePart;
import fr.sncf.osrd.envelope_sim.EnvelopeProfile;
import fr.sncf.osrd.infra.implementation.tracks.directed.TrackRangeView;
import fr.sncf.osrd.infra_state.api.TrainPath;
import fr.sncf.osrd.train.RollingStock;
import java.util.List;

/** MRSP = most restrictive speed profile: maximum speed allowed at any given point */
public class MRSP {
    public enum LimitKind implements EnvelopeAttr {
        SPEED_LIMIT,
        TRAIN_LIMIT,
        ;

        @Override
        public Class<? extends EnvelopeAttr> getAttrType() {
            return LimitKind.class;
        }
    }

    /** Computes the most restricted speed profile from a path */
    public static Envelope from(TrainPath trainPath, RollingStock rollingStock, boolean addRollingStockLength) {
        return from(TrainPath.removeLocation(trainPath.trackRangePath()), rollingStock, addRollingStockLength);
    }

    /** Computes the most restricted speed profile from a list of track ranges */
    public static Envelope from(List<TrackRangeView> ranges, RollingStock rollingStock, boolean addRollingStockLength) {
        var builder = new MRSPEnvelopeBuilder();
        var pathLength = 0.;
        for (var r : ranges)
            pathLength += r.getLength();

        // add a limit for the maximum speed the hardware is rated for
        builder.addPart(EnvelopePart.generateTimes(
                List.of(EnvelopeProfile.CONSTANT_SPEED, LimitKind.TRAIN_LIMIT),
                new double[] { 0, pathLength },
                new double[] { rollingStock.maxSpeed, rollingStock.maxSpeed }
        ));

        var offset = 0.;
        for (var range : ranges) {
            if (range.getLength() == 0)
                continue;
            for (var speedRange : range.getSpeedSections().getValuesInRange(0, range.getLength()).entrySet()) {
                // compute where this limit is active from and to
                var interval = speedRange.getKey();
                var begin = offset + interval.getBeginPosition();
                var end = offset + interval.getEndPosition();
                if (addRollingStockLength) {
                    end += rollingStock.length;
                    if (end > pathLength)
                        end = pathLength;
                }
                var speed = speedRange.getValue();
                if (speed.isInfinite() || speed == 0)
                    continue;

                // Add the envelope part corresponding to the restricted speed section
                builder.addPart(EnvelopePart.generateTimes(
                        List.of(EnvelopeProfile.CONSTANT_SPEED, LimitKind.SPEED_LIMIT),
                        new double[]{begin, end},
                        new double[]{speed, speed}
                ));
            }
            offset += range.getLength();
        }
        return builder.build();
    }
}
