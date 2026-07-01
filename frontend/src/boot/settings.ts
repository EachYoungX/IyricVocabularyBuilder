import { defineBoot } from '#q-app/wrappers';
import { applyAppSettings, loadAppSettings } from 'src/utils/appSettings';

export default defineBoot(() => {
  applyAppSettings(loadAppSettings());
});
