package fr.sncf.osrd.envelope_sim;

import fr.sncf.osrd.envelope.EnvelopeAttr;

public class StopMeta implements EnvelopeAttr {
    public final int stopIndex;

    public StopMeta(int stopIndex) {
        this.stopIndex = stopIndex;
    }
}
