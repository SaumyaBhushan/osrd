package fr.sncf.osrd.railjson.schema.schedule;

import com.squareup.moshi.Json;
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory;

public class RJSAllowance {
    public static final PolymorphicJsonAdapterFactory<RJSAllowance> adapter = (
            PolymorphicJsonAdapterFactory.of(RJSAllowance.class, "allowance_type")
                    .withSubtype(Construction.class, "construction")
                    .withSubtype(Mareco.class, "mareco")
    );

    public static final class Construction extends RJSAllowance {
        @Json(name = "begin_position")
        public double beginPosition = Double.NaN;
        @Json(name = "end_position")
        public double endPosition = Double.NaN;
        @Json(name = "capacity_speed_limit")
        public double capacitySpeedLimit = -1;
        public RJSAllowanceValue value;
    }

    public static final class Mareco extends RJSAllowance {
        @Json(name = "default_value")
        public RJSAllowanceValue defaultValue;

        public RJSAllowanceRange[] ranges;

        @Json(name = "capacity_speed_limit")
        public double capacitySpeedLimit = -1;

        public Mareco(RJSAllowanceValue defaultValue) {
            this.defaultValue = defaultValue;
            this.ranges = null;
        }

        public Mareco(RJSAllowanceValue defaultValue, RJSAllowanceRange[] ranges) {
            this.defaultValue = defaultValue;
            this.ranges = ranges;
        }
    }
}
