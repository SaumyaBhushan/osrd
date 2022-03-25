package fr.sncf.osrd.new_infra.implementation.reservation;

import static fr.sncf.osrd.new_infra.api.Direction.BACKWARD;
import static fr.sncf.osrd.new_infra.api.Direction.FORWARD;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.graph.ImmutableNetwork;
import com.google.common.graph.NetworkBuilder;
import fr.sncf.osrd.new_infra.api.Direction;
import fr.sncf.osrd.new_infra.api.reservation.*;
import fr.sncf.osrd.new_infra.api.tracks.directed.DiTrackInfra;
import fr.sncf.osrd.new_infra.api.tracks.undirected.TrackObject;
import fr.sncf.osrd.new_infra.implementation.GraphHelpers;
import fr.sncf.osrd.new_infra.implementation.RJSObjectParsing;
import fr.sncf.osrd.new_infra.implementation.tracks.directed.DirectedInfraBuilder;
import fr.sncf.osrd.new_infra.implementation.tracks.directed.TrackRangeView;
import fr.sncf.osrd.railjson.schema.infra.RJSInfra;
import fr.sncf.osrd.railjson.schema.infra.RJSRoute;
import fr.sncf.osrd.utils.graph.EdgeDirection;
import java.util.*;

public class ReservationInfraBuilder {

    private final DiTrackInfra diTrackInfra;
    private final RJSInfra rjsInfra;
    private Map<String, DetectorImpl> detectorMap;
    private Map<Direction, Map<String, DiDetector>> diDetectorMap;

    /** Constructor */
    private ReservationInfraBuilder(RJSInfra rjsInfra, DiTrackInfra infra) {
        this.rjsInfra = rjsInfra;
        this.diTrackInfra = infra;
    }

    /** Builds a ReservationInfra from railjson data and a DiTrackInfra */
    public static ReservationInfra fromDiTrackInfra(RJSInfra rjsInfra, DiTrackInfra diTrackInfra) {
        return new ReservationInfraBuilder(rjsInfra, diTrackInfra).build();
    }

    /** Builds a ReservationInfra from a railjson infra */
    public static ReservationInfra fromRJS(RJSInfra rjsInfra) {
        var diInfra = DirectedInfraBuilder.fromRJS(rjsInfra);
        return fromDiTrackInfra(rjsInfra, diInfra);
    }

    /** Builds everything */
    private ReservationInfra build() {
        var detectorMaps = DetectorMaps.from(diTrackInfra);
        detectorMap = detectorMaps.detectorMap;
        diDetectorMap = detectorMaps.diDetectorMap;
        var reservationSections = DetectionSectionBuilder.build(
                diTrackInfra,
                diDetectorMap
        );
        var routeGraph = makeRouteGraph();
        return new ReservationInfraImpl(
                diTrackInfra,
                ImmutableMap.copyOf(detectorMap),
                convertDiDetectorMap(),
                makeSectionMap(reservationSections),
                routeGraph,
                makeReservationRouteMap(routeGraph)
        );
    }

    /** Builds an ID to route mapping */
    private ImmutableMap<String, ReservationRoute> makeReservationRouteMap(
            ImmutableNetwork<DiDetector, ReservationRoute> routeGraph
    ) {
        var res = ImmutableMap.<String, ReservationRoute>builder();
        for (var route : routeGraph.edges())
            res.put(route.getID(), route);
        return res.build();
    }

    /** Converts the map of map into an immutableMap of immutableMap */
    private ImmutableMap<Direction, ImmutableMap<String, DiDetector>> convertDiDetectorMap() {
        var builder = ImmutableMap.<Direction, ImmutableMap<String, DiDetector>>builder();
        for (var entry : diDetectorMap.entrySet())
            builder.put(entry.getKey(), ImmutableMap.copyOf(entry.getValue()));
        return builder.build();
    }

    /** Instantiates the routes and links them together in the graph */
    private ImmutableNetwork<DiDetector, ReservationRoute> makeRouteGraph() {
        var networkBuilder = NetworkBuilder
                .directed()
                .<DiDetector, ReservationRoute>immutable();
        var routesPerSection
                = HashMultimap.<DetectionSection, ReservationRouteImpl>create();
        var routes = new ArrayList<ReservationRouteImpl>();
        for (var rjsRoute : rjsInfra.routes) {
            var trackRanges = makeTrackRanges(rjsRoute);
            var route = new ReservationRouteImpl(detectorsOnRoute(rjsRoute), releasePoints(rjsRoute),
                    rjsRoute.id, trackRanges, isPassive(rjsRoute, trackRanges));
            routes.add(route);
            for (var section : sectionsOnRoute(rjsRoute)) {
                routesPerSection.put(section, route);
            }
        }
        Map<ReservationRouteImpl, Set<ReservationRoute>> routeConflictBuilders = new HashMap<>();
        for (var routesSharingSection : routesPerSection.asMap().values()) {
            for (var route : routesSharingSection) {
                var conflictSet = routeConflictBuilders.computeIfAbsent(route, r -> new HashSet<>());
                for (var otherRoute : routesSharingSection)
                    if (otherRoute != route)
                        conflictSet.add(otherRoute);
            }
        }
        for (var route : routes) {
            route.setConflictingRoutes(routeConflictBuilders.get(route));
            networkBuilder.addEdge(
                    route.getDetectorPath().get(0),
                    route.getDetectorPath().get(route.getDetectorPath().size() - 1),
                    route
            );
        }
        return networkBuilder.build();
    }

    /** Returns true if the route is passive */
    private boolean isPassive(RJSRoute route, ImmutableList<TrackRangeView> trackPath) {
        if (route.isControlled != null)
            return !route.isControlled;
        for (int i = 1; i < trackPath.size(); i++) {
            var prev = trackPath.get(i - 1).track.getEdge();
            var next = trackPath.get(i).track.getEdge();
            if (prev != next) {
                var node = GraphHelpers.getCommonNode(diTrackInfra.getTrackGraph(), prev, next);
                var isSwitch = diTrackInfra.getTrackGraph().degree(node) > 2;
                if (isSwitch)
                    return false;
            }
        }
        return true;
    }

    private ImmutableList<TrackRangeView> makeTrackRanges(RJSRoute rjsRoute) {
        var res = ImmutableList.<TrackRangeView>builder();
        for (var range : rjsRoute.path) {
            range.track.checkType(Set.of("TrackSection"));
            res.add(new TrackRangeView(
                    range.begin,
                    range.end,
                    diTrackInfra.getEdge(range.track.id.id, Direction.fromEdgeDir(range.direction))
            ));
        }
        return res.build();
    }

    /** Lists the release points on a given route (in order) */
    private ImmutableList<Detector> releasePoints(RJSRoute rjsRoute) {
        var builder = ImmutableList.<Detector>builder();
        for (var detector : rjsRoute.releaseDetectors) {
            builder.add(RJSObjectParsing.getDetector(detector, detectorMap));
        }
        return builder.build();
    }

    /** Creates a set of detection section present in the route */
    private Set<DetectionSection> sectionsOnRoute(RJSRoute route) {
        var res = new HashSet<DetectionSection>();

        var detectors = detectorsOnRoute(route);
        for (int i = 0; i < detectors.size() - 1; i++) {
            var d = detectors.get(i);
            res.add(d.getDetector().getNextDetectionSection(d.getDirection()));
        }
        return res;
    }

    /** Creates the list of DiDetectors present on the route */
    private ImmutableList<DiDetector> detectorsOnRoute(RJSRoute route) {
        var res = new ArrayList<DiDetector>();
        for (var trackRange : route.path) {
            var min = Math.min(trackRange.begin, trackRange.end);
            var max = Math.max(trackRange.begin, trackRange.end);
            var track = RJSObjectParsing.getTrackSection(trackRange.track, diTrackInfra);
            var objectsOnTrack = new ArrayList<TrackObject>();
            for (var object : track.getTrackObjects()) {
                if (min <= object.getOffset() && object.getOffset() <= max)
                    objectsOnTrack.add(object);
            }
            if (trackRange.direction.equals(EdgeDirection.START_TO_STOP)) {
                for (var o : objectsOnTrack)
                    res.add(diDetectorMap.get(FORWARD).get(o.getID()));
            } else {
                for (int i = objectsOnTrack.size() - 1; i >= 0; i--)
                    res.add(diDetectorMap.get(BACKWARD).get(objectsOnTrack.get(i).getID()));
            }
        }
        return ImmutableList.copyOf(res);
    }

    /** Creates a mapping from a directed detector to its next detection section */
    private ImmutableMap<DiDetector, DetectionSection> makeSectionMap(ArrayList<DetectionSection> sections) {
        var builder = ImmutableMap.<DiDetector, DetectionSection>builder();
        for (var section : sections) {
            for (var diDetector : section.getDetectors()) {
                builder.put(diDetector, section);
            }
        }
        return builder.buildOrThrow();
    }
}
