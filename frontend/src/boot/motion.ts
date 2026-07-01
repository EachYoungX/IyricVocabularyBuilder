import { defineBoot } from '#q-app/wrappers';
import {
  applyMotionPreference,
  setMotionPreference,
  toggleMotionPreference,
  type MotionPreference,
} from 'src/utils/motionPreference';

declare global {
  interface Window {
    setLyricVocabularyMotion?: (preference: MotionPreference) => void;
    toggleLyricVocabularyMotion?: () => MotionPreference;
  }
}

export default defineBoot(() => {
  applyMotionPreference();

  window.setLyricVocabularyMotion = (preference: MotionPreference) => {
    setMotionPreference(preference);
  };

  window.toggleLyricVocabularyMotion = () => {
    return toggleMotionPreference();
  };
});
