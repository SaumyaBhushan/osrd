package fr.sncf.osrd.envelope;

public interface EnvelopeAttr {
    /** Returns a slice of the attribute */
    default EnvelopeAttr slice(double beginPos, double endPos) {
        return this;
    }
}
