import React from 'react';
import nextId from 'react-id-generator';
import { sec2time } from 'utils/timeManipulation';
import { useSelector } from 'react-redux';
import { useTranslation } from 'react-i18next';

function formatStops(stop, idx, train) {
  return (
    <tr key={nextId()}>
      <td>
        <div className="cell-inner font-weight-bold">
          {stop.name || 'Unknown'}
        </div>
      </td>
      <td><div className="cell-inner">{sec2time(stop.time)}</div></td>
      <td><div className="cell-inner">{stop.duration > 0 && sec2time(stop.time + stop.duration)}</div></td>
      <td>
        <div className="cell-inner">
          {stop.duration > 0 ? `${stop.duration}s` : null}
        </div>
      </td>
      <td>
        <div className="cell-inner">
          {train.eco
          && sec2time(train.eco.stops[idx].time)}
        </div>
      </td>
      <td>
        <div className="cell-inner">
          {train.eco && train.eco.stops[idx].duration > 0
            && sec2time(train.eco.stops[idx].time + train.eco.stops[idx].duration)}
        </div>
      </td>
    </tr>
  );
}

export default function TimeTable() {
  const { t } = useTranslation(['simulation']);
  const { selectedTrain } = useSelector((state) => state.osrdsimulation);
  const simulation = useSelector((state) => state.osrdsimulation.simulation.present);
  const data = simulation.trains[selectedTrain].base.stops;

  return (
    <>
      <div className="h2">{t('simulation:timetable')}</div>
      <div className="table-wrapper">
        <div className="table-scroller dragscroll">
          <table className="table table-hover table-shrink">
            <thead className="thead thead-light">
              <tr>
                <th scope="col"><div className="cell-inner">{t('simulation:stopPlace')}</div></th>
                <th scope="col"><div className="cell-inner">{t('simulation:stopTime')}</div></th>
                <th scope="col"><div className="cell-inner">{t('simulation:departureTime')}</div></th>
                <th scope="col"><div className="cell-inner">{t('simulation:stopStopTime')}</div></th>
                <th scope="col"><div className="cell-inner">{`${t('simulation:stopTime')} ECO`}</div></th>
                <th scope="col"><div className="cell-inner">{`${t('simulation:departureTime')} ECO`}</div></th>
              </tr>
            </thead>
            <tbody>
              {data.map((stop, idx) => formatStops(stop, idx, simulation.trains[selectedTrain]))}
            </tbody>
          </table>
        </div>
      </div>
    </>
  );
}
