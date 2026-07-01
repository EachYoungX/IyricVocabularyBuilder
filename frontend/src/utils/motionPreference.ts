export const MOTION_STORAGE_KEY = 'lv-motion';

export type MotionPreference = 'on' | 'off';

export function getStoredMotionPreference(): MotionPreference | null {
  const value = window.localStorage.getItem(MOTION_STORAGE_KEY);
  return value === 'on' || value === 'off' ? value : null;
}

export function applyMotionPreference(preference?: MotionPreference | null) {
  const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  const motion = preference ?? getStoredMotionPreference() ?? (reducedMotion ? 'off' : 'on');
  document.documentElement.dataset.motion = motion;
  return motion;
}

export function setMotionPreference(preference: MotionPreference) {
  window.localStorage.setItem(MOTION_STORAGE_KEY, preference);
  return applyMotionPreference(preference);
}

export function toggleMotionPreference() {
  return setMotionPreference(document.documentElement.dataset.motion === 'off' ? 'on' : 'off');
}
