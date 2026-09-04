import axios from 'axios';
import { OpenAPI } from 'src/services/api';

export type BackupPayload = {
  schemaVersion: number;
  appVersion: string;
  exportedAt: string;
  songs: unknown[];
  userVocabulary: unknown[];
  userPhrases: unknown[];
  vocabularyOverrides: unknown[];
  settings?: unknown;
  motionPreference?: 'on' | 'off';
};

export type BackupValidationResult = {
  valid: boolean;
  schemaVersion: number;
  songCount: number;
  userVocabularyCount: number;
  userPhraseCount: number;
};

export type BackupRestoreResult = {
  restoredSongs: number;
  restoredUserVocabulary: number;
  restoredUserPhrases: number;
  vocabularyRebuildTaskId: string;
};

type ApiEnvelope<T> = {
  code: number;
  message: string;
  data: T;
};

function apiUrl(path: string) {
  return `${OpenAPI.BASE}${path}`;
}

export const BackupApiService = {
  async exportBackup() {
    const response = await axios.get<ApiEnvelope<BackupPayload>>(apiUrl('/api/backup/export'));
    return response.data.data;
  },

  async validateBackup(payload: unknown) {
    const response = await axios.post<ApiEnvelope<BackupValidationResult>>(
      apiUrl('/api/backup/validate'),
      payload,
    );
    return response.data.data;
  },

  async restoreBackup(payload: unknown) {
    const response = await axios.post<ApiEnvelope<BackupRestoreResult>>(
      apiUrl('/api/backup/restore?mode=overwrite'),
      payload,
    );
    return response.data.data;
  },
};
