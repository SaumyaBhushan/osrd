package fr.sncf.osrd.envelope;

import java.util.HashMap;
import java.util.Map;

/** A container for envelope metadata */
public final class EnvelopePartMeta {
    private final Map<Class<? extends EnvelopeAttr>, EnvelopeAttr> attrs;

    public EnvelopePartMeta() {
        this.attrs = new HashMap<>();
    }

    public EnvelopePartMeta(EnvelopePartMeta o) {
        this.attrs = new HashMap<>(o.attrs);
    }

    public EnvelopePartMeta(Map<Class<? extends EnvelopeAttr>, EnvelopeAttr> attrs) {
        this.attrs = new HashMap<>(attrs);
    }

    /** Create an envelope part meta with a single attribute */
    public EnvelopePartMeta(Class<? extends EnvelopeAttr> attrType, EnvelopeAttr attrValue) {
        var attrs = makeAttrMap();
        attrs.put(attrType, attrValue);
        this.attrs = attrs;
    }

    /** Returns the attribute for the given type, or null */
    @SuppressWarnings({"unchecked"})
    public <T extends EnvelopeAttr> T get(Class<T> attrType) {
        return (T) attrs.get(attrType);
    }

    /** Create a new empty attribute map */
    public static Map<Class<? extends EnvelopeAttr>, EnvelopeAttr> makeAttrMap() {
        return new HashMap<>();
    }

    /** Return a new envelope part meta with sliced attributes */
    public EnvelopePartMeta slice(double beginPos, double endPos) {
        var res = new EnvelopePartMeta();
        for (var attr : attrs.entrySet()) {
            var attrType = attr.getKey();
            var oldValue = attr.getValue();
            var newValue = oldValue.slice(beginPos, endPos);
            if (newValue == null)
                continue;
            res.attrs.put(attrType, newValue);
        }
        return res;
    }

    /** Creates a copy of this envelope part meta, with overridden attributes */
    public EnvelopePartMeta override(Map<Class<? extends EnvelopeAttr>, EnvelopeAttr> attrOverrides) {
        var res = new EnvelopePartMeta(this);
        res.attrs.putAll(attrOverrides);
        return res;
    }

    /** Creates a copy of this envelope part meta, with overridden attributes */
    public EnvelopePartMeta override(Class<? extends EnvelopeAttr> attrType, EnvelopeAttr attrValue) {
        var res = new EnvelopePartMeta(this);
        res.attrs.put(attrType, attrValue);
        return res;
    }
}
