package fr.sncf.osrd.railjson.schema.infra.signaling;

import com.squareup.moshi.Json;
import fr.sncf.osrd.railjson.schema.common.ID;

import java.util.Collection;

public class SignalingData {
    /** Extra data needed for BAPR signals */
    public static class BAPRData extends SignalingData {
        /** True if the signal has a red aspect */
        @Json(name = "has_red")
        public boolean hasRed;

        /** True if the signal has a yellow aspect */
        @Json(name = "has_yellow")
        public boolean hasYellow;
    }

    /** Extra data needed for TVM signals */
    public static class TVMData extends SignalingData {

        /** All master signals to which this signal is a slave. One entry per pair of associated aspect */
        public Collection<MasterSignal> masterSignals;

        /** One master / slave link between two signals for two given aspects */
        public static class MasterSignal {
            /** ID of the referenced master signal */
            @Json(name = "has_yellow")
            public ID<RJSSignal> signalID;

            /** This state on the slave signal is caused by the given aspect on the master signal */
            @Json(name = "slave_state")
            public String slaveState;

            /** This state on the master signal causes the given aspect on the slave signal */
            @Json(name = "slave_state")
            public int masterState;
        }
    }
}
