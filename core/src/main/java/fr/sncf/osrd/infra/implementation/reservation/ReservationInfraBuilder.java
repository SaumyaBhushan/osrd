package fr.sncf.osrd.infra.implementation.reservation;

import static fr.sncf.osrd.infra.api.Direction.BACKWARD;
import static fr.sncf.osrd.infra.api.Direction.FORWARD;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableNetwork;
import com.google.common.graph.NetworkBuilder;
import fr.sncf.osrd.infra.api.Direction;
import fr.sncf.osrd.infra.api.reservation.DetectionSection;
import fr.sncf.osrd.infra.api.reservation.DiDetector;
import fr.sncf.osrd.infra.api.reservation.ReservationInfra;
import fr.sncf.osrd.infra.api.reservation.ReservationRoute;
import fr.sncf.osrd.infra.api.tracks.directed.DiTrackEdge;
import fr.sncf.osrd.infra.api.tracks.directed.DiTrackInfra;
import fr.sncf.osrd.infra.api.tracks.undirected.Detector;
import fr.sncf.osrd.infra.api.tracks.undirected.SwitchBranch;
import fr.sncf.osrd.infra.errors.DiscontinuousRoute;
import fr.sncf.osrd.infra.implementation.RJSObjectParsing;
import fr.sncf.osrd.infra.implementation.tracks.directed.DirectedInfraBuilder;
import fr.sncf.osrd.infra.implementation.tracks.directed.TrackRangeView;
import fr.sncf.osrd.railjson.schema.common.graph.EdgeEndpoint;
import fr.sncf.osrd.railjson.schema.infra.RJSInfra;
import fr.sncf.osrd.railjson.schema.infra.RJSRoute;
import fr.sncf.osrd.reporting.warnings.Warning;
import fr.sncf.osrd.reporting.warnings.WarningRecorder;
import fr.sncf.osrd.utils.graph.GraphHelpers;
import java.util.*;

public class ReservationInfraBuilder {

    private final DiTrackInfra diTrackInfra;
    private final RJSInfra rjsInfra;
    private final WarningRecorder warningRecorder;

    /** Constructor */
    private ReservationInfraBuilder(RJSInfra rjsInfra, DiTrackInfra infra, WarningRecorder warningRecorder) {
        this.rjsInfra = rjsInfra;
        this.diTrackInfra = infra;
        this.warningRecorder = warningRecorder;
    }

    /** Builds a ReservationInfra from railjson data and a DiTrackInfra */
    public static ReservationInfra fromDiTrackInfra(
            RJSInfra rjsInfra,
            DiTrackInfra diTrackInfra,
            WarningRecorder warningRecorder
    ) {
        return new ReservationInfraBuilder(rjsInfra, diTrackInfra, warningRecorder).build();
    }

    /** Builds a ReservationInfra from a railjson infra */
    public static ReservationInfra fromRJS(RJSInfra rjsInfra, WarningRecorder warningRecorder) {
        var diInfra = DirectedInfraBuilder.fromRJS(rjsInfra, warningRecorder);
        return fromDiTrackInfra(rjsInfra, diInfra, warningRecorder);
    }

    /** Builds everything */
    private ReservationInfra build() {
        var reservationSections = DetectionSectionBuilder.build(
                diTrackInfra
        );
        var routeGraph = makeRouteGraph(reservationSections);
        return new ReservationInfraImpl(
                diTrackInfra,
                makeSectionMap(reservationSections),
                routeGraph,
                makeReservationRouteMap(routeGraph),
                makeRouteOnEdgeMap(routeGraph.edges())
        );
    }

    /** Builds a multimap of routes present on each edge */
    private static ImmutableMultimap<DiTrackEdge, ReservationInfra.RouteEntry>
            makeRouteOnEdgeMap(Collection<ReservationRoute> routes) {
        var res = ImmutableMultimap.<DiTrackEdge, ReservationInfra.RouteEntry>builder();
        for (var route : routes) {
            var offset = 0;
            for (var range : route.getTrackRanges()) {
                double rangeOffset; // Offset from the start of the track to the start of the range
                if (range.track.getDirection().equals(FORWARD))
                    rangeOffset = range.begin;
                else
                    rangeOffset = range.track.getEdge().getLength() - range.end;
                assert rangeOffset == 0 || offset == 0
                        : "A track range must be either the first on the route, or start at the edge of a track";
                var rangeOffsetFromRouteStart = rangeOffset - offset;
                var routeEntry = new ReservationInfra.RouteEntry(route, rangeOffsetFromRouteStart);
                res.put(range.track, routeEntry);
                offset += range.getLength();
            }
        }
        return res.build();
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

    /** Instantiates the routes and links them together in the graph */
    private ImmutableNetwork<DiDetector, ReservationRoute> makeRouteGraph(
            ArrayList<DetectionSection> reservationSections
    ) {
        var networkBuilder = NetworkBuilder
                .directed()
                .<DiDetector, ReservationRoute>immutable();
        var routesPerSection = new IdentityHashMap<DetectionSection, ImmutableSet.Builder<ReservationRoute>>();
        for (var section : reservationSections)
            routesPerSection.put(section, new ImmutableSet.Builder<>());
        var routes = new ArrayList<ReservationRouteImpl>();
        for (var rjsRoute : rjsInfra.routes) {
            var trackRanges = makeTrackRanges(rjsRoute);
            var length = trackRanges.stream().mapToDouble(TrackRangeView::getLength).sum();
            var detectors = detectorsOnRoute(trackRanges);
            var sections = sectionsOnRoute(detectors);
            var route = new ReservationRouteImpl(detectors, releasePoints(rjsRoute),
                    rjsRoute.id, trackRanges, isRouteControlled(trackRanges), length, sections);
            validateRoute(route, rjsRoute);
            routes.add(route);
            for (var section : sections)
                routesPerSection.get(section).add(route);
        }

        // Sets the routes passing through each section
        for (var entry : routesPerSection.entrySet())
            if (entry.getKey() instanceof DetectionSectionImpl section)
                section.setRoutes(entry.getValue().build());

        for (var route : routes) {
            networkBuilder.addEdge(
                    route.getDetectorPath().get(0),
                    route.getDetectorPath().get(route.getDetectorPath().size() - 1),
                    route
            );
        }
        return networkBuilder.build();
    }

    private void validateRoute(ReservationRouteImpl route, RJSRoute rjsRoute) {
        if (route.getDetectorPath().isEmpty()) {
            warningRecorder.register(new Warning(String.format(
                    "Route %s has no detector on its track ranges", rjsRoute.id
            )));
            return;
        }
        var detectorIDs = rjsRoute.releaseDetectors.stream()
                        .map(x -> x.id.id)
                        .toList();
        var lastDetector = route.getDetectorPath().get(route.getDetectorPath().size() - 1);
        if (rjsRoute.entryPoint != null
                && !route.getDetectorPath().get(0).detector().getID().equals(rjsRoute.entryPoint.id.id))
            warningRecorder.register(new Warning(String.format(
                    "Entry point for route %s don't match the first detector on the route "
                            + "(expected = %s, detector list = %s)",
                    rjsRoute.id,
                    rjsRoute.entryPoint.id.id,
                    detectorIDs
            )));
        if (rjsRoute.exitPoint != null
                && !lastDetector.detector().getID().equals(rjsRoute.exitPoint.id.id)) {
            warningRecorder.register(new Warning(String.format(
                    "Exit point for route %s don't match the last detector on the route "
                            + "(expected = %s, detector list = %s)",
                    rjsRoute.id,
                    rjsRoute.exitPoint.id.id,
                    detectorIDs
            )));
        }
    }

    /** Returns true if the route is controlled (requires explicit reservation to be used) */
    private boolean isRouteControlled(ImmutableList<TrackRangeView> trackRanges) {
        // TODO: eventually, add an optional parameter to RJSRoute
        return trackRanges.stream()
                .anyMatch(trackRangeView -> trackRangeView.track.getEdge() instanceof SwitchBranch);
    }

    private ImmutableList<TrackRangeView> makeTrackRanges(RJSRoute rjsRoute) {
        var res = new ArrayList<TrackRangeView>();
        for (var range : rjsRoute.path) {
            range.track.checkType(Set.of("TrackSection"));
            res.add(new TrackRangeView(
                    range.begin,
                    range.end,
                    diTrackInfra.getEdge(range.track.id.id, Direction.fromEdgeDir(range.direction))
            ));
        }

        // Adds switch branches edges (unspecified in the RJS path)
        for (int i = 1; i < res.size(); i++) {
            var prev = res.get(i - 1);
            var next = res.get(i);
            var g = diTrackInfra.getTrackGraph();
            if (g.adjacentEdges(prev.track.getEdge()).contains(next.track.getEdge()))
                continue;
            var prevNode = GraphHelpers.nodeFromEdgeEndpoint(
                    g,
                    prev.track.getEdge(),
                    EdgeEndpoint.endEndpoint(prev.track.getDirection())
            );
            var nextNode = GraphHelpers.nodeFromEdgeEndpoint(
                    g,
                    next.track.getEdge(),
                    EdgeEndpoint.startEndpoint(next.track.getDirection())
            );
            var connecting = g.edgeConnecting(prevNode, nextNode);
            if (connecting.isEmpty())
                connecting = g.edgeConnecting(nextNode, prevNode);
            if (connecting.isEmpty())
                throw new DiscontinuousRoute(rjsRoute.id, prev.track.getEdge().getID(), next.track.getEdge().getID());
            var branchEdge = connecting.get();
            var dir = g.incidentNodes(branchEdge).nodeU().equals(prevNode) ? FORWARD : BACKWARD;
            res.add(i, new TrackRangeView(0, 0, diTrackInfra.getEdge(branchEdge, dir)));
        }
        return ImmutableList.copyOf(res);
    }

    /** Lists the release points on a given route (in order) */
    private ImmutableList<Detector> releasePoints(RJSRoute rjsRoute) {
        var builder = ImmutableList.<Detector>builder();
        for (var detector : rjsRoute.releaseDetectors) {
            builder.add(RJSObjectParsing.getDetector(detector, diTrackInfra.getDetectorMap()));
        }
        return builder.build();
    }

    /** Creates a set of detection section present in the route */
    private ImmutableList<DetectionSection> sectionsOnRoute(ImmutableList<DiDetector> detectors) {
        var res = ImmutableList.<DetectionSection>builder();

        for (int i = 0; i < detectors.size() - 1; i++) {
            var d = detectors.get(i);
            res.add(d.detector().getNextDetectionSection(d.direction()));
        }
        return res.build();
    }

    /** Creates the list of DiDetectors present on the route */
    private ImmutableList<DiDetector> detectorsOnRoute(List<TrackRangeView> ranges) {
        var res = ImmutableList.<DiDetector>builder();
        for (var range : ranges)
            for (var detector : range.getDetectors())
                res.add(detector.element().getDiDetector(range.track.getDirection()));
        return res.build();
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
