# Generated by Django 3.2.9 on 2022-03-25 13:41

import django.contrib.gis.db.models.fields
import django.contrib.postgres.fields
import django.db.models.deletion
from django.db import migrations, models

import osrd_infra.utils


def run_sql_add_foreign_key_infra(model_name: str):
    return migrations.RunSQL(
            f"""ALTER TABLE osrd_infra_{model_name}
                ADD infra_id INTEGER,
                ADD CONSTRAINT osrd_infra_{model_name}_fkey FOREIGN KEY (infra_id) REFERENCES osrd_infra_infra(id) ON DELETE CASCADE
            """,
            state_operations=[
                migrations.AddField(
                    model_name=model_name,
                    name="infra",
                    field=models.ForeignKey(
                        on_delete=django.db.models.deletion.CASCADE,
                        to="osrd_infra.infra",
                    ),
                ),
            ],
        )


class Migration(migrations.Migration):

    initial = True

    dependencies = []

    operations = [
        migrations.CreateModel(
            name='Infra',
            fields=[
                ('id', models.AutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('name', models.CharField(max_length=128)),
                ('railjson_version', models.CharField(default='2.2.0', editable=False, max_length=16)),
                ('owner', models.UUIDField(default='00000000-0000-0000-0000-000000000000', editable=False)),
                ('version', models.CharField(default='1', editable=False, max_length=40)),
                ('generated_version', models.CharField(editable=False, max_length=40, null=True)),
            ],
        ),
        migrations.CreateModel(
            name='PathModel',
            fields=[
                ('id', models.AutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('owner', models.UUIDField(default='00000000-0000-0000-0000-000000000000', editable=False)),
                ('created', models.DateTimeField(auto_now_add=True)),
                ('payload', models.JSONField(validators=[osrd_infra.utils.JSONSchemaValidator(limit_value={'definitions': {'Direction': {'description': 'An enumeration.', 'enum': ['START_TO_STOP', 'STOP_TO_START'], 'title': 'Direction', 'type': 'string'}, 'DirectionalTrackRange': {'properties': {'begin': {'title': 'Begin', 'type': 'number'}, 'direction': {'$ref': '#/definitions/Direction'}, 'end': {'title': 'End', 'type': 'number'}, 'track': {'$ref': '#/definitions/ObjectReference'}}, 'required': ['track', 'begin', 'end', 'direction'], 'title': 'DirectionalTrackRange', 'type': 'object'}, 'ObjectReference': {'properties': {'id': {'maxLength': 255, 'title': 'Id', 'type': 'string'}, 'type': {'title': 'Type', 'type': 'string'}}, 'required': ['id', 'type'], 'title': 'ObjectReference', 'type': 'object'}, 'PathWaypoint': {'properties': {'duration': {'title': 'Duration', 'type': 'number'}, 'geo': {'$ref': '#/definitions/Point'}, 'name': {'title': 'Name', 'type': 'string'}, 'position': {'title': 'Position', 'type': 'number'}, 'sch': {'$ref': '#/definitions/Point'}, 'suggestion': {'title': 'Suggestion', 'type': 'boolean'}, 'track': {'$ref': '#/definitions/ObjectReference'}}, 'required': ['track', 'position', 'geo', 'sch', 'suggestion', 'duration'], 'title': 'PathWaypoint', 'type': 'object'}, 'Point': {'description': 'Point Model', 'properties': {'coordinates': {'anyOf': [{'items': [{'anyOf': [{'type': 'number'}, {'type': 'integer'}]}, {'anyOf': [{'type': 'number'}, {'type': 'integer'}]}], 'maxItems': 2, 'minItems': 2, 'type': 'array'}, {'items': [{'anyOf': [{'type': 'number'}, {'type': 'integer'}]}, {'anyOf': [{'type': 'number'}, {'type': 'integer'}]}, {'anyOf': [{'type': 'number'}, {'type': 'integer'}]}], 'maxItems': 3, 'minItems': 3, 'type': 'array'}], 'title': 'Coordinates'}, 'type': {'const': 'Point', 'title': 'Type', 'type': 'string'}}, 'required': ['coordinates'], 'title': 'Point', 'type': 'object'}, 'RoutePath': {'properties': {'route': {'$ref': '#/definitions/ObjectReference'}, 'track_sections': {'items': {'$ref': '#/definitions/DirectionalTrackRange'}, 'title': 'Track Sections', 'type': 'array'}}, 'required': ['route', 'track_sections'], 'title': 'RoutePath', 'type': 'object'}}, 'properties': {'path_waypoints': {'items': {'$ref': '#/definitions/PathWaypoint'}, 'title': 'Path Waypoints', 'type': 'array'}, 'route_paths': {'items': {'$ref': '#/definitions/RoutePath'}, 'title': 'Route Paths', 'type': 'array'}}, 'required': ['route_paths', 'path_waypoints'], 'title': 'PathPayload', 'type': 'object'})])),
                ('slopes', models.JSONField(validators=[osrd_infra.utils.JSONSchemaValidator(limit_value={'definitions': {'SlopePoint': {'properties': {'gradient': {'title': 'Gradient', 'type': 'number'}, 'position': {'title': 'Position', 'type': 'number'}}, 'required': ['position', 'gradient'], 'title': 'SlopePoint', 'type': 'object'}}, 'items': {'$ref': '#/definitions/SlopePoint'}, 'title': 'Slopes', 'type': 'array'})])),
                ('curves', models.JSONField(validators=[osrd_infra.utils.JSONSchemaValidator(limit_value={'definitions': {'CurvePoint': {'properties': {'position': {'title': 'Position', 'type': 'number'}, 'radius': {'title': 'Radius', 'type': 'number'}}, 'required': ['position', 'radius'], 'title': 'CurvePoint', 'type': 'object'}}, 'items': {'$ref': '#/definitions/CurvePoint'}, 'title': 'Curves', 'type': 'array'})])),
                ('geographic', django.contrib.gis.db.models.fields.LineStringField(srid=4326)),
                ('schematic', django.contrib.gis.db.models.fields.LineStringField(srid=4326)),
            ],
            options={
                'verbose_name_plural': 'paths',
            },
        ),
        migrations.CreateModel(
            name='RollingStock',
            fields=[
                ('id', models.AutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('name', models.CharField(help_text='A unique identifier for this rolling stock', max_length=255, unique=True)),
                ('owner', models.UUIDField(default='00000000-0000-0000-0000-000000000000', editable=False)),
                ('length', models.FloatField(help_text='The length of the train, in meters')),
                ('mass', models.FloatField(help_text='The mass of the train, in kilograms')),
                ('inertia_coefficient', models.FloatField(help_text='The inertia coefficient. It will be multiplied with the mass of the train to get its effective mass')),
                ('rolling_resistance', models.JSONField(help_text='The formula to use to compute rolling resistance', validators=[osrd_infra.utils.JSONSchemaValidator(limit_value={'properties': {'A': {'minimum': 0, 'type': 'number'}, 'B': {'minimum': 0, 'type': 'number'}, 'C': {'minimum': 0, 'type': 'number'}, 'type': {'const': 'davis'}}, 'required': ['type', 'A', 'B', 'C'], 'type': 'object'})])),
                ('capabilities', django.contrib.postgres.fields.ArrayField(base_field=models.CharField(max_length=255), help_text='A list of features the train exhibits, such as ERTMS support', size=None)),
                ('max_speed', models.FloatField(help_text='The maximum operational speed, in m/s')),
                ('startup_time', models.FloatField(help_text='The time the train takes before it can start accelerating')),
                ('startup_acceleration', models.FloatField(help_text='The maximum acceleration during startup, in m/s^2')),
                ('comfort_acceleration', models.FloatField(help_text='The maximum operational acceleration, in m/s^2')),
                ('timetable_gamma', models.FloatField(help_text='The maximum braking coefficient, for timetabling purposes, in m/s^2')),
                ('tractive_effort_curves', models.JSONField(help_text='A set of curves mapping speed (in m/s) to maximum traction (in newtons)', validators=[osrd_infra.utils.JSONSchemaValidator(limit_value={'additionalProperties': {'items': {'properties': {'max_effort': {'type': 'number'}, 'speed': {'type': 'number'}}, 'required': ['speed', 'max_effort'], 'type': 'object'}, 'title': 'schema', 'type': 'array'}, 'type': 'object'})])),
                ('traction_mode', models.CharField(max_length=128)),
                ('power_class', models.PositiveIntegerField()),
                ('image', models.ImageField(blank=True, null=True, upload_to='')),
            ],
        ),
        migrations.CreateModel(
            name='Timetable',
            fields=[
                ('id', models.AutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('name', models.CharField(max_length=128)),
            ],
        ),
        migrations.CreateModel(
            name='TrainScheduleModel',
            fields=[
                ('id', models.AutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('train_name', models.CharField(max_length=128)),
                ('departure_time', models.FloatField()),
                ('initial_speed', models.FloatField()),
                ('labels', models.JSONField(default=[], validators=[osrd_infra.utils.JSONSchemaValidator(limit_value={'items': {'maxLength': 128, 'type': 'string'}, 'title': 'TrainScheduleLabels', 'type': 'array'})])),
                ('allowances', models.JSONField(default=[], validators=[osrd_infra.utils.JSONSchemaValidator(limit_value={'definitions': {'Allowance': {'anyOf': [{'$ref': '#/definitions/ConstructionAllowance'}, {'$ref': '#/definitions/MarecoAllowance'}], 'discriminator': {'mapping': {'construction': '#/definitions/ConstructionAllowance', 'mareco': '#/definitions/MarecoAllowance'}, 'propertyName': 'allowance_type'}, 'title': 'Allowance'}, 'AllowancePercentValue': {'properties': {'percentage': {'title': 'Percentage', 'type': 'number'}, 'value_type': {'default': 'percentage', 'enum': ['percentage'], 'title': 'Value Type', 'type': 'string'}}, 'required': ['percentage'], 'title': 'AllowancePercentValue', 'type': 'object'}, 'AllowanceTimePerDistanceValue': {'properties': {'minutes': {'description': 'min/100km', 'title': 'Minutes', 'type': 'number'}, 'value_type': {'default': 'time_per_distance', 'enum': ['time_per_distance'], 'title': 'Value Type', 'type': 'string'}}, 'required': ['minutes'], 'title': 'AllowanceTimePerDistanceValue', 'type': 'object'}, 'AllowanceTimeValue': {'properties': {'seconds': {'title': 'Seconds', 'type': 'number'}, 'value_type': {'default': 'time', 'enum': ['time'], 'title': 'Value Type', 'type': 'string'}}, 'required': ['seconds'], 'title': 'AllowanceTimeValue', 'type': 'object'}, 'AllowanceValue': {'anyOf': [{'$ref': '#/definitions/AllowanceTimeValue'}, {'$ref': '#/definitions/AllowancePercentValue'}, {'$ref': '#/definitions/AllowanceTimePerDistanceValue'}], 'discriminator': {'mapping': {'percentage': '#/definitions/AllowancePercentValue', 'time': '#/definitions/AllowanceTimeValue', 'time_per_distance': '#/definitions/AllowanceTimePerDistanceValue'}, 'propertyName': 'value_type'}, 'title': 'AllowanceValue'}, 'ConstructionAllowance': {'properties': {'allowance_type': {'default': 'construction', 'enum': ['construction'], 'title': 'Allowance Type', 'type': 'string'}, 'begin_position': {'title': 'Begin Position', 'type': 'number'}, 'end_position': {'title': 'End Position', 'type': 'number'}, 'value': {'$ref': '#/definitions/AllowanceValue'}}, 'required': ['begin_position', 'end_position', 'value'], 'title': 'ConstructionAllowance', 'type': 'object'}, 'MarecoAllowance': {'properties': {'allowance_type': {'default': 'mareco', 'enum': ['mareco'], 'title': 'Allowance Type', 'type': 'string'}, 'default_value': {'$ref': '#/definitions/AllowanceValue'}, 'ranges': {'items': {'$ref': '#/definitions/RangeAllowance'}, 'title': 'Ranges', 'type': 'array'}}, 'required': ['default_value', 'ranges'], 'title': 'MarecoAllowance', 'type': 'object'}, 'RangeAllowance': {'properties': {'begin_position': {'title': 'Begin Position', 'type': 'number'}, 'end_position': {'title': 'End Position', 'type': 'number'}, 'value': {'$ref': '#/definitions/AllowanceValue'}}, 'required': ['begin_position', 'end_position', 'value'], 'title': 'RangeAllowance', 'type': 'object'}}, 'items': {'$ref': '#/definitions/Allowance'}, 'title': 'Allowances', 'type': 'array'})])),
                ('mrsp', models.JSONField(validators=[osrd_infra.utils.JSONSchemaValidator(limit_value={'definitions': {'MRSPPoint': {'properties': {'position': {'title': 'Position', 'type': 'number'}, 'speed': {'title': 'Speed', 'type': 'number'}}, 'required': ['position', 'speed'], 'title': 'MRSPPoint', 'type': 'object'}}, 'items': {'$ref': '#/definitions/MRSPPoint'}, 'title': 'MRPS', 'type': 'array'})])),
                ('base_simulation', models.JSONField()),
                ('eco_simulation', models.JSONField(null=True)),
                ('rolling_stock', models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, to='osrd_infra.rollingstock')),
            ],
        ),
        migrations.RunSQL(
            """ALTER TABLE osrd_infra_trainschedulemodel
                ADD path_id INTEGER,
                ADD CONSTRAINT osrd_infra_trainschedulemodel_path_fkey FOREIGN KEY (path_id) REFERENCES osrd_infra_pathmodel(id) ON DELETE CASCADE
            """,
            state_operations=[
                migrations.AddField(
                    model_name="trainschedulemodel",
                    name="path",
                    field=models.ForeignKey(
                        on_delete=django.db.models.deletion.CASCADE,
                        to="osrd_infra.pathmodel",
                    ),
                ),
            ],
        ),
        migrations.RunSQL(
            """ALTER TABLE osrd_infra_trainschedulemodel
                ADD timetable_id INTEGER,
                ADD CONSTRAINT osrd_infra_trainschedulemodel_timetable_fkey FOREIGN KEY (timetable_id) REFERENCES osrd_infra_timetable(id) ON DELETE CASCADE
            """,
            state_operations=[
                migrations.AddField(
                    model_name="trainschedulemodel",
                    name="timetable",
                    field=models.ForeignKey(
                        on_delete=django.db.models.deletion.CASCADE,
                        to="osrd_infra.timetable",
                        related_name="train_schedules",
                    ),
                ),
            ],
        ),
        migrations.CreateModel(
            name='TrackSectionModel',
            fields=[
                ('id', models.AutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('obj_id', models.CharField(max_length=255)),
                ('data', models.JSONField(validators=[osrd_infra.utils.JSONSchemaValidator(limit_value={'definitions': {'ApplicableDirections': {'description': 'An enumeration.', 'enum': ['START_TO_STOP', 'STOP_TO_START', 'BOTH'], 'title': 'ApplicableDirections', 'type': 'string'}, 'Curve': {'properties': {'begin': {'title': 'Begin', 'type': 'number'}, 'end': {'title': 'End', 'type': 'number'}, 'radius': {'title': 'Radius', 'type': 'number'}}, 'required': ['radius', 'begin', 'end'], 'title': 'Curve', 'type': 'object'}, 'LineString': {'description': 'LineString Model', 'properties': {'coordinates': {'items': {'anyOf': [{'items': [{'anyOf': [{'type': 'number'}, {'type': 'integer'}]}, {'anyOf': [{'type': 'number'}, {'type': 'integer'}]}], 'maxItems': 2, 'minItems': 2, 'type': 'array'}, {'items': [{'anyOf': [{'type': 'number'}, {'type': 'integer'}]}, {'anyOf': [{'type': 'number'}, {'type': 'integer'}]}, {'anyOf': [{'type': 'number'}, {'type': 'integer'}]}], 'maxItems': 3, 'minItems': 3, 'type': 'array'}]}, 'minItems': 2, 'title': 'Coordinates', 'type': 'array'}, 'type': {'const': 'LineString', 'title': 'Type', 'type': 'string'}}, 'required': ['coordinates'], 'title': 'LineString', 'type': 'object'}, 'Slope': {'properties': {'begin': {'title': 'Begin', 'type': 'number'}, 'end': {'title': 'End', 'type': 'number'}, 'gradient': {'title': 'Gradient', 'type': 'number'}}, 'required': ['gradient', 'begin', 'end'], 'title': 'Slope', 'type': 'object'}}, 'properties': {'curves': {'items': {'$ref': '#/definitions/Curve'}, 'title': 'Curves', 'type': 'array'}, 'geo': {'$ref': '#/definitions/LineString'}, 'id': {'maxLength': 255, 'title': 'Id', 'type': 'string'}, 'length': {'title': 'Length', 'type': 'number'}, 'line_code': {'title': 'Line Code', 'type': 'integer'}, 'line_name': {'maxLength': 255, 'title': 'Line Name', 'type': 'string'}, 'navigability': {'$ref': '#/definitions/ApplicableDirections'}, 'sch': {'$ref': '#/definitions/LineString'}, 'slopes': {'items': {'$ref': '#/definitions/Slope'}, 'title': 'Slopes', 'type': 'array'}, 'track_name': {'maxLength': 255, 'title': 'Track Name', 'type': 'string'}, 'track_number': {'title': 'Track Number', 'type': 'integer'}}, 'required': ['geo', 'sch', 'id', 'length', 'line_code', 'line_name', 'track_number', 'track_name', 'navigability', 'slopes', 'curves'], 'title': 'TrackSection', 'type': 'object'})])),
            ],
            options={
                'verbose_name_plural': 'track sections',
            },
        ),
        migrations.CreateModel(
            name='TrackSectionLinkModel',
            fields=[
                ('id', models.AutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('obj_id', models.CharField(max_length=255)),
                ('data', models.JSONField(validators=[osrd_infra.utils.JSONSchemaValidator(limit_value={'definitions': {'ApplicableDirections': {'description': 'An enumeration.', 'enum': ['START_TO_STOP', 'STOP_TO_START', 'BOTH'], 'title': 'ApplicableDirections', 'type': 'string'}, 'Endpoint': {'description': 'An enumeration.', 'enum': ['BEGIN', 'END'], 'title': 'Endpoint', 'type': 'string'}, 'ObjectReference': {'properties': {'id': {'maxLength': 255, 'title': 'Id', 'type': 'string'}, 'type': {'title': 'Type', 'type': 'string'}}, 'required': ['id', 'type'], 'title': 'ObjectReference', 'type': 'object'}, 'TrackEndpoint': {'properties': {'endpoint': {'$ref': '#/definitions/Endpoint'}, 'track': {'$ref': '#/definitions/ObjectReference'}}, 'required': ['endpoint', 'track'], 'title': 'TrackEndpoint', 'type': 'object'}}, 'properties': {'dst': {'$ref': '#/definitions/TrackEndpoint'}, 'id': {'maxLength': 255, 'title': 'Id', 'type': 'string'}, 'navigability': {'$ref': '#/definitions/ApplicableDirections'}, 'src': {'$ref': '#/definitions/TrackEndpoint'}}, 'required': ['id', 'src', 'dst', 'navigability'], 'title': 'TrackSectionLink', 'type': 'object'})])),
            ],
            options={
                'verbose_name_plural': 'track section links',
            },
        ),
        migrations.CreateModel(
            name='TrackSectionLayer',
            fields=[
                ('id', models.AutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('obj_id', models.CharField(max_length=255)),
                ('geographic', django.contrib.gis.db.models.fields.LineStringField(srid=3857)),
                ('schematic', django.contrib.gis.db.models.fields.LineStringField(srid=3857)),
            ],
            options={
                'verbose_name_plural': 'generated track sections layer',
            },
        ),
        migrations.CreateModel(
            name='SwitchTypeModel',
            fields=[
                ('id', models.AutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('obj_id', models.CharField(max_length=255)),
                ('data', models.JSONField(validators=[osrd_infra.utils.JSONSchemaValidator(limit_value={'definitions': {'SwitchPortConnection': {'properties': {'bidirectional': {'title': 'Bidirectional', 'type': 'boolean'}, 'dst': {'title': 'Dst', 'type': 'string'}, 'src': {'title': 'Src', 'type': 'string'}}, 'required': ['src', 'dst', 'bidirectional'], 'title': 'SwitchPortConnection', 'type': 'object'}}, 'properties': {'groups': {'additionalProperties': {'items': {'$ref': '#/definitions/SwitchPortConnection'}, 'type': 'array'}, 'title': 'Groups', 'type': 'object'}, 'id': {'maxLength': 255, 'title': 'Id', 'type': 'string'}, 'ports': {'items': {'type': 'string'}, 'title': 'Ports', 'type': 'array'}}, 'required': ['id', 'ports', 'groups'], 'title': 'SwitchType', 'type': 'object'})])),
            ],
            options={
                'verbose_name_plural': 'switch types',
            },
        ),
        migrations.CreateModel(
            name='SwitchModel',
            fields=[
                ('id', models.AutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('obj_id', models.CharField(max_length=255)),
                ('data', models.JSONField(validators=[osrd_infra.utils.JSONSchemaValidator(limit_value={'definitions': {'Endpoint': {'description': 'An enumeration.', 'enum': ['BEGIN', 'END'], 'title': 'Endpoint', 'type': 'string'}, 'ObjectReference': {'properties': {'id': {'maxLength': 255, 'title': 'Id', 'type': 'string'}, 'type': {'title': 'Type', 'type': 'string'}}, 'required': ['id', 'type'], 'title': 'ObjectReference', 'type': 'object'}, 'TrackEndpoint': {'properties': {'endpoint': {'$ref': '#/definitions/Endpoint'}, 'track': {'$ref': '#/definitions/ObjectReference'}}, 'required': ['endpoint', 'track'], 'title': 'TrackEndpoint', 'type': 'object'}}, 'properties': {'group_change_delay': {'title': 'Group Change Delay', 'type': 'number'}, 'id': {'maxLength': 255, 'title': 'Id', 'type': 'string'}, 'ports': {'additionalProperties': {'$ref': '#/definitions/TrackEndpoint'}, 'title': 'Ports', 'type': 'object'}, 'switch_type': {'$ref': '#/definitions/ObjectReference'}}, 'required': ['id', 'switch_type', 'group_change_delay', 'ports'], 'title': 'Switch', 'type': 'object'})])),
            ],
            options={
                'verbose_name_plural': 'switches',
            },
        ),
        migrations.CreateModel(
            name='SpeedSectionModel',
            fields=[
                ('id', models.AutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('obj_id', models.CharField(max_length=255)),
                ('data', models.JSONField(validators=[osrd_infra.utils.JSONSchemaValidator(limit_value={'definitions': {'ApplicableDirections': {'description': 'An enumeration.', 'enum': ['START_TO_STOP', 'STOP_TO_START', 'BOTH'], 'title': 'ApplicableDirections', 'type': 'string'}, 'ApplicableDirectionsTrackRange': {'properties': {'applicable_directions': {'$ref': '#/definitions/ApplicableDirections'}, 'begin': {'title': 'Begin', 'type': 'number'}, 'end': {'title': 'End', 'type': 'number'}, 'track': {'$ref': '#/definitions/ObjectReference'}}, 'required': ['track', 'begin', 'end', 'applicable_directions'], 'title': 'ApplicableDirectionsTrackRange', 'type': 'object'}, 'ObjectReference': {'properties': {'id': {'maxLength': 255, 'title': 'Id', 'type': 'string'}, 'type': {'title': 'Type', 'type': 'string'}}, 'required': ['id', 'type'], 'title': 'ObjectReference', 'type': 'object'}}, 'properties': {'id': {'maxLength': 255, 'title': 'Id', 'type': 'string'}, 'speed': {'title': 'Speed', 'type': 'number'}, 'track_ranges': {'items': {'$ref': '#/definitions/ApplicableDirectionsTrackRange'}, 'title': 'Track Ranges', 'type': 'array'}}, 'required': ['id', 'speed', 'track_ranges'], 'title': 'SpeedSection', 'type': 'object'})])),
            ],
            options={
                'verbose_name_plural': 'speed sections',
            },
        ),
        migrations.CreateModel(
            name='SpeedSectionLayer',
            fields=[
                ('id', models.AutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('obj_id', models.CharField(max_length=255)),
                ('geographic', django.contrib.gis.db.models.fields.MultiLineStringField(srid=3857)),
                ('schematic', django.contrib.gis.db.models.fields.MultiLineStringField(srid=3857)),
            ],
            options={
                'verbose_name_plural': 'generated speed sections layer',
            },
        ),
        migrations.CreateModel(
            name='SignalModel',
            fields=[
                ('id', models.AutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('obj_id', models.CharField(max_length=255)),
                ('data', models.JSONField(validators=[osrd_infra.utils.JSONSchemaValidator(limit_value={'definitions': {'Direction': {'description': 'An enumeration.', 'enum': ['START_TO_STOP', 'STOP_TO_START'], 'title': 'Direction', 'type': 'string'}, 'ObjectReference': {'properties': {'id': {'maxLength': 255, 'title': 'Id', 'type': 'string'}, 'type': {'title': 'Type', 'type': 'string'}}, 'required': ['id', 'type'], 'title': 'ObjectReference', 'type': 'object'}}, 'properties': {'angle_geo': {'title': 'Angle Geo', 'type': 'number'}, 'angle_sch': {'title': 'Angle Sch', 'type': 'number'}, 'aspects': {'items': {'type': 'string'}, 'title': 'Aspects', 'type': 'array'}, 'comment': {'title': 'Comment', 'type': 'string'}, 'direction': {'$ref': '#/definitions/Direction'}, 'id': {'maxLength': 255, 'title': 'Id', 'type': 'string'}, 'installation_type': {'title': 'Installation Type', 'type': 'string'}, 'is_in_service': {'title': 'Is In Service', 'type': 'boolean'}, 'is_lightable': {'title': 'Is Lightable', 'type': 'boolean'}, 'is_operational': {'title': 'Is Operational', 'type': 'boolean'}, 'label': {'title': 'Label', 'type': 'string'}, 'linked_detector': {'$ref': '#/definitions/ObjectReference'}, 'physical_organization_group': {'title': 'Physical Organization Group', 'type': 'string'}, 'position': {'title': 'Position', 'type': 'number'}, 'responsible_group': {'title': 'Responsible Group', 'type': 'string'}, 'sight_distance': {'title': 'Sight Distance', 'type': 'number'}, 'support_type': {'title': 'Support Type', 'type': 'string'}, 'track': {'$ref': '#/definitions/ObjectReference'}, 'type_code': {'title': 'Type Code', 'type': 'string'}, 'value': {'title': 'Value', 'type': 'string'}}, 'required': ['track', 'position', 'id', 'direction', 'sight_distance'], 'title': 'Signal', 'type': 'object'})])),
            ],
            options={
                'verbose_name_plural': 'signals',
            },
        ),
        migrations.CreateModel(
            name='SignalLayer',
            fields=[
                ('id', models.AutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('obj_id', models.CharField(max_length=255)),
                ('geographic', django.contrib.gis.db.models.fields.PointField(srid=3857)),
                ('schematic', django.contrib.gis.db.models.fields.PointField(srid=3857)),
            ],
            options={
                'verbose_name_plural': 'generated signals layer',
            },
        ),
        migrations.CreateModel(
            name='RouteModel',
            fields=[
                ('id', models.AutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('obj_id', models.CharField(max_length=255)),
                ('data', models.JSONField(validators=[osrd_infra.utils.JSONSchemaValidator(limit_value={'definitions': {'Direction': {'description': 'An enumeration.', 'enum': ['START_TO_STOP', 'STOP_TO_START'], 'title': 'Direction', 'type': 'string'}, 'DirectionalTrackRange': {'properties': {'begin': {'title': 'Begin', 'type': 'number'}, 'direction': {'$ref': '#/definitions/Direction'}, 'end': {'title': 'End', 'type': 'number'}, 'track': {'$ref': '#/definitions/ObjectReference'}}, 'required': ['track', 'begin', 'end', 'direction'], 'title': 'DirectionalTrackRange', 'type': 'object'}, 'ObjectReference': {'properties': {'id': {'maxLength': 255, 'title': 'Id', 'type': 'string'}, 'type': {'title': 'Type', 'type': 'string'}}, 'required': ['id', 'type'], 'title': 'ObjectReference', 'type': 'object'}}, 'properties': {'entry_point': {'$ref': '#/definitions/ObjectReference'}, 'exit_point': {'$ref': '#/definitions/ObjectReference'}, 'id': {'maxLength': 255, 'title': 'Id', 'type': 'string'}, 'path': {'items': {'$ref': '#/definitions/DirectionalTrackRange'}, 'title': 'Path', 'type': 'array'}, 'release_detectors': {'items': {'$ref': '#/definitions/ObjectReference'}, 'title': 'Release Detectors', 'type': 'array'}}, 'required': ['id', 'entry_point', 'exit_point', 'release_detectors', 'path'], 'title': 'Route', 'type': 'object'})])),
            ],
            options={
                'verbose_name_plural': 'routes',
            },
        ),
        migrations.CreateModel(
            name='OperationalPointModel',
            fields=[
                ('id', models.AutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('obj_id', models.CharField(max_length=255)),
                ('data', models.JSONField(validators=[osrd_infra.utils.JSONSchemaValidator(limit_value={'definitions': {'ObjectReference': {'properties': {'id': {'maxLength': 255, 'title': 'Id', 'type': 'string'}, 'type': {'title': 'Type', 'type': 'string'}}, 'required': ['id', 'type'], 'title': 'ObjectReference', 'type': 'object'}, 'OperationalPointPart': {'properties': {'position': {'title': 'Position', 'type': 'number'}, 'track': {'$ref': '#/definitions/ObjectReference'}}, 'required': ['track', 'position'], 'title': 'OperationalPointPart', 'type': 'object'}}, 'properties': {'ch': {'maxLength': 2, 'title': 'Ch', 'type': 'string'}, 'ch_long_label': {'maxLength': 255, 'title': 'Ch Long Label', 'type': 'string'}, 'ch_short_label': {'maxLength': 255, 'title': 'Ch Short Label', 'type': 'string'}, 'ci': {'title': 'Ci', 'type': 'integer'}, 'id': {'maxLength': 255, 'title': 'Id', 'type': 'string'}, 'name': {'maxLength': 255, 'title': 'Name', 'type': 'string'}, 'parts': {'items': {'$ref': '#/definitions/OperationalPointPart'}, 'title': 'Parts', 'type': 'array'}}, 'required': ['id', 'parts', 'ci', 'ch', 'name'], 'title': 'OperationalPoint', 'type': 'object'})])),
            ],
            options={
                'verbose_name_plural': 'operational points',
            },
        ),
        migrations.CreateModel(
            name='DetectorModel',
            fields=[
                ('id', models.AutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('obj_id', models.CharField(max_length=255)),
                ('data', models.JSONField(validators=[osrd_infra.utils.JSONSchemaValidator(limit_value={'definitions': {'ApplicableDirections': {'description': 'An enumeration.', 'enum': ['START_TO_STOP', 'STOP_TO_START', 'BOTH'], 'title': 'ApplicableDirections', 'type': 'string'}, 'ObjectReference': {'properties': {'id': {'maxLength': 255, 'title': 'Id', 'type': 'string'}, 'type': {'title': 'Type', 'type': 'string'}}, 'required': ['id', 'type'], 'title': 'ObjectReference', 'type': 'object'}}, 'properties': {'applicable_directions': {'$ref': '#/definitions/ApplicableDirections'}, 'id': {'maxLength': 255, 'title': 'Id', 'type': 'string'}, 'position': {'title': 'Position', 'type': 'number'}, 'track': {'$ref': '#/definitions/ObjectReference'}}, 'required': ['track', 'position', 'id', 'applicable_directions'], 'title': 'Detector', 'type': 'object'})])),
            ],
            options={
                'verbose_name_plural': 'detectors',
            },
        ),
        migrations.CreateModel(
            name='CatenaryModel',
            fields=[
                ('id', models.AutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('obj_id', models.CharField(max_length=255)),
                ('data', models.JSONField(validators=[osrd_infra.utils.JSONSchemaValidator(limit_value={'definitions': {'ApplicableDirections': {'description': 'An enumeration.', 'enum': ['START_TO_STOP', 'STOP_TO_START', 'BOTH'], 'title': 'ApplicableDirections', 'type': 'string'}, 'ApplicableDirectionsTrackRange': {'properties': {'applicable_directions': {'$ref': '#/definitions/ApplicableDirections'}, 'begin': {'title': 'Begin', 'type': 'number'}, 'end': {'title': 'End', 'type': 'number'}, 'track': {'$ref': '#/definitions/ObjectReference'}}, 'required': ['track', 'begin', 'end', 'applicable_directions'], 'title': 'ApplicableDirectionsTrackRange', 'type': 'object'}, 'ObjectReference': {'properties': {'id': {'maxLength': 255, 'title': 'Id', 'type': 'string'}, 'type': {'title': 'Type', 'type': 'string'}}, 'required': ['id', 'type'], 'title': 'ObjectReference', 'type': 'object'}}, 'properties': {'id': {'maxLength': 255, 'title': 'Id', 'type': 'string'}, 'track_ranges': {'items': {'$ref': '#/definitions/ApplicableDirectionsTrackRange'}, 'title': 'Track Ranges', 'type': 'array'}, 'voltage': {'title': 'Voltage', 'type': 'number'}}, 'required': ['id', 'voltage', 'track_ranges'], 'title': 'Catenary', 'type': 'object'})])),
            ],
            options={
                'verbose_name_plural': 'catenaries',
            },
        ),
        migrations.CreateModel(
            name='BufferStopModel',
            fields=[
                ('id', models.AutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('obj_id', models.CharField(max_length=255)),
                ('data', models.JSONField(validators=[osrd_infra.utils.JSONSchemaValidator(limit_value={'definitions': {'ApplicableDirections': {'description': 'An enumeration.', 'enum': ['START_TO_STOP', 'STOP_TO_START', 'BOTH'], 'title': 'ApplicableDirections', 'type': 'string'}, 'ObjectReference': {'properties': {'id': {'maxLength': 255, 'title': 'Id', 'type': 'string'}, 'type': {'title': 'Type', 'type': 'string'}}, 'required': ['id', 'type'], 'title': 'ObjectReference', 'type': 'object'}}, 'properties': {'applicable_directions': {'$ref': '#/definitions/ApplicableDirections'}, 'id': {'maxLength': 255, 'title': 'Id', 'type': 'string'}, 'position': {'title': 'Position', 'type': 'number'}, 'track': {'$ref': '#/definitions/ObjectReference'}}, 'required': ['track', 'position', 'id', 'applicable_directions'], 'title': 'BufferStop', 'type': 'object'})])),
            ],
            options={
                'verbose_name_plural': 'buffer stops',
            },
        ),
        migrations.CreateModel(
            name='ErrorLayer',
            fields=[
                ('id', models.AutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('obj_type', models.CharField(choices=[('OperationalPoint', 'OperationalPoint'), ('Route', 'Route'), ('SwitchType', 'SwitchType'), ('Switch', 'Switch'), ('TrackSectionLink', 'TrackSectionLink'), ('SpeedSection', 'SpeedSection'), ('Catenary', 'Catenary'), ('TrackSection', 'TrackSection'), ('Signal', 'Signal'), ('BufferStop', 'BufferStop'), ('Detector', 'Detector')], max_length=32)),
                ('obj_id', models.CharField(max_length=255)),
                ('geographic', django.contrib.gis.db.models.fields.GeometryField(null=True, srid=3857)),
                ('schematic', django.contrib.gis.db.models.fields.GeometryField(null=True, srid=3857)),
                ('information', models.JSONField(validators=[osrd_infra.utils.JSONSchemaValidator(limit_value={'anyOf': [{'$ref': '#/definitions/InvalidReference'}, {'$ref': '#/definitions/OutOfRange'}, {'$ref': '#/definitions/EmptyObject'}], 'definitions': {'EmptyObject': {'properties': {'error_type': {'default': 'empty_object', 'enum': ['empty_object'], 'title': 'Error Type', 'type': 'string'}, 'field': {'title': 'Field', 'type': 'string'}, 'is_warning': {'default': True, 'enum': [True], 'title': 'Is Warning', 'type': 'boolean'}}, 'required': ['field'], 'title': 'EmptyObject', 'type': 'object'}, 'InvalidReference': {'properties': {'error_type': {'default': 'invalid_reference', 'enum': ['invalid_reference'], 'title': 'Error Type', 'type': 'string'}, 'field': {'title': 'Field', 'type': 'string'}, 'is_warning': {'default': False, 'enum': [False], 'title': 'Is Warning', 'type': 'boolean'}, 'reference': {'$ref': '#/definitions/ObjectReference'}}, 'required': ['field', 'reference'], 'title': 'InvalidReference', 'type': 'object'}, 'ObjectReference': {'properties': {'id': {'maxLength': 255, 'title': 'Id', 'type': 'string'}, 'type': {'title': 'Type', 'type': 'string'}}, 'required': ['id', 'type'], 'title': 'ObjectReference', 'type': 'object'}, 'OutOfRange': {'properties': {'error_type': {'default': 'out_of_range', 'enum': ['out_of_range'], 'title': 'Error Type', 'type': 'string'}, 'expected_range': {'items': [{'type': 'number'}, {'type': 'number'}], 'maxItems': 2, 'minItems': 2, 'title': 'Expected Range', 'type': 'array'}, 'field': {'title': 'Field', 'type': 'string'}, 'is_warning': {'default': False, 'enum': [False], 'title': 'Is Warning', 'type': 'boolean'}, 'position': {'title': 'Position', 'type': 'number'}}, 'required': ['field', 'position', 'expected_range'], 'title': 'OutOfRange', 'type': 'object'}}, 'discriminator': {'mapping': {'empty_object': '#/definitions/EmptyObject', 'invalid_reference': '#/definitions/InvalidReference', 'out_of_range': '#/definitions/OutOfRange'}, 'propertyName': 'error_type'}, 'title': 'InfraError'})])),
            ],
            options={
                'verbose_name_plural': 'generated errors',
            },
        ),
    ]

    infra_foreign_models = ["pathmodel", "timetable", "errorlayer"]
    infra_obj_models = [
        "routemodel",
        "operationalpointmodel",
        "detectormodel",
        "catenarymodel",
        "bufferstopmodel",
        "tracksectionmodel",
        "tracksectionlinkmodel",
        "tracksectionlayer",
        "switchtypemodel",
        "switchmodel",
        "speedsectionmodel",
        "speedsectionlayer",
        "signalmodel",
        "signallayer",
    ]

    for model in infra_foreign_models + infra_obj_models:
        operations.append(run_sql_add_foreign_key_infra(model))

    for model in infra_obj_models:
        operations.append(
            migrations.AlterUniqueTogether(
                name=model,
                unique_together={('infra', 'obj_id')},
            )
        )
