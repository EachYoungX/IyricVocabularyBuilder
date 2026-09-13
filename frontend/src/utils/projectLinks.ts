export const PROJECT_URL = 'https://github.com/EachYoungX/IyricVocabularyBuilder';
export const DICTIONARY_DOWNLOAD_URL = 'https://github.com/EachYoungX/IyricVocabularyBuilder-Dictionary/releases';

export async function openProjectPage(event: MouseEvent, page: 'project' | 'dictionary') {
  if (!window.desktopBridge) return; // Normal browsers follow the anchor directly.
  event.preventDefault();
  await window.desktopBridge.openProjectPage(page);
}
