import requests
from django.conf import settings
from rest_framework import mixins
from rest_framework.decorators import action
from rest_framework.exceptions import ParseError
from rest_framework.generics import get_object_or_404
from rest_framework.response import Response
from rest_framework.viewsets import GenericViewSet

from osrd_infra.serializers import TrainScheduleSerializer
from osrd_infra.views import get_rolling_stock_payload, get_train_schedule_payload
from osrd_infra.views.projection import Projection
from osrd_infra.utils import geo_transform

from osrd_infra.models import (
    TrackSectionEntity,
    TrainSchedule,
    TrainScheduleResult,
    entities_prefetch_components,
)


def format_result(train_schedule_result):
    steps = format_steps(train_schedule_result)
    return {
        "name": train_schedule_result.train_schedule.train_id,
        "steps": steps,
        "stops": format_stops(train_schedule_result, steps),
    }


def format_steps(train_schedule_result):
    routes = train_schedule_result.train_schedule.path.payload["path"]
    path = []
    tracks = set()
    for route in routes:
        path += route["track_sections"]
        [tracks.add(track["track_section"]) for track in route["track_sections"]]
    projection = Projection(path)
    res = []
    qs = TrackSectionEntity.objects.filter(pk__in=list(tracks))
    prefetch_tracks = entities_prefetch_components(TrackSectionEntity, qs)
    tracks = {track.pk: track for track in prefetch_tracks}
    for log in train_schedule_result.log:
        if log["type"] != "train_location":
            continue
        head_track_id = int(log["head_track_section"].split(".")[1])
        tail_track_id = int(log["tail_track_section"].split(".")[1])
        head_track = tracks[head_track_id]
        geo_line = geo_transform(head_track.geo_line_location.geographic)
        schema_line = geo_transform(head_track.geo_line_location.schematic)
        head_offset_normalized = log["head_offset"] / head_track.track_section.length
        res.append(
            {
                "time": log["time"],
                "speed": log["speed"],
                "head_position": projection.track_position(
                    head_track_id, log["head_offset"]
                ),
                "tail_position": projection.track_position(
                    tail_track_id, log["tail_offset"]
                ),
                "geo_position": geo_line.interpolate_normalized(head_offset_normalized).json,
                "schema_position": schema_line.interpolate_normalized(
                    head_offset_normalized
                ).json,
            }
        )
    return res


def format_stops(train_schedule_result, steps):
    op_times = {}
    for log in train_schedule_result.log:
        if log["type"] == "operational_point":
            op_id = int(log["operational_point"].split(".")[1])
            op_times[op_id] = log["time"]
    stops = [
        {
            "name": "start",
            "time": train_schedule_result.train_schedule.departure_time,
            "stop_time": 0,
        }
    ]
    for phase in train_schedule_result.train_schedule.phases:
        stops.append(
            {
                "name": phase["operational_point"],
                "time": op_times.get(phase["operational_point"], float("nan")),
                "stop_time": phase["stop_time"],
            }
        )
    stops.append(
        {
            "name": "stop",
            "time": steps[-1]["time"],
            "stop_time": 0,
        }
    )
    return stops


class TrainScheduleView(
    mixins.CreateModelMixin,
    mixins.RetrieveModelMixin,
    mixins.UpdateModelMixin,
    mixins.DestroyModelMixin,
    GenericViewSet,
):
    queryset = TrainSchedule.objects.all()
    serializer_class = TrainScheduleSerializer

    @action(detail=True, methods=["get"])
    def result(self, request, pk=None):
        train_schedule = self.get_object()
        result = get_object_or_404(TrainScheduleResult, train_schedule=train_schedule)
        return Response(format_result(result))

    @action(detail=True, methods=["post"])
    def run(self, request, pk=None):
        train_schedule = self.get_object()
        payload = {
            "infra": train_schedule.timetable.infra_id,
            "rolling_stocks": [get_rolling_stock_payload(train_schedule.rolling_stock)],
            "train_schedules": [get_train_schedule_payload(train_schedule)],
        }

        try:
            response = requests.post(
                settings.OSRD_BACKEND_URL + "simulation",
                headers={"Authorization": "Bearer " + settings.OSRD_BACKEND_TOKEN},
                json=payload,
            )
        except requests.exceptions.ConnectionError:
            raise ParseError("Couldn't connect with osrd backend")

        if not response:
            raise ParseError(response.content)
        result, _ = TrainScheduleResult.objects.get_or_create(
            train_schedule=train_schedule, log=response.json()
        )
        result.save()
        return Response(format_result(result))
