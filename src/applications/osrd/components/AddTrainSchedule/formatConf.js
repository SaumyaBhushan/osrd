export default function formatConf(dispatch, setFailure, t, osrdconf, originTime) {
  let error = false;
  if (!osrdconf.origin) {
    error = true;
    dispatch(setFailure({
      name: t('osrdconf:errorMessages.title'),
      message: t('osrdconf:errorMessages.noOrigin'),
    }));
  }
  if (!osrdconf.originTime) {
    error = true;
    dispatch(setFailure({
      name: t('osrdconf:errorMessages.title'),
      message: t('osrdconf:errorMessages.noOriginTime'),
    }));
  }
  if (!osrdconf.destination) {
    error = true;
    dispatch(setFailure({
      name: t('osrdconf:errorMessages.title'),
      message: t('osrdconf:errorMessages.noDestination'),
    }));
  }
  if (!osrdconf.rollingStockID) {
    error = true;
    dispatch(setFailure({
      name: t('osrdconf:errorMessages.title'),
      message: t('osrdconf:errorMessages.noRollingStock'),
    }));
  }
  if (!osrdconf.name) {
    error = true;
    dispatch(setFailure({
      name: t('osrdconf:errorMessages.title'),
      message: t('osrdconf:errorMessages.noName'),
    }));
  }
  if (!osrdconf.timetableID) {
    error = true;
    dispatch(setFailure({
      name: t('osrdconf:errorMessages.title'),
      message: t('osrdconf:errorMessages.noTimetable'),
    }));
  }

  if (!error) {
    const osrdConfSchedule = {
      train_name: osrdconf.name,
      departure_time: originTime,
      phases: [],
      initial_speed: 0,
      timetable: osrdconf.timetableID,
      rolling_stock: osrdconf.rollingStockID,
      path: osrdconf.pathfindingID,
    };
    return osrdConfSchedule;
  }
  return false;
}
