/**
 * 扩展的歌曲导入请求类型，支持语言检测标记
 */
import type { SongImportRequest } from 'src/services/api';

export interface ExtendedSongImportRequest extends SongImportRequest {
  /**
   * 是否为非英语歌词
   */
  isNonEnglish?: boolean;

  /**
   * 语言警告信息
   */
  languageWarning?: string | undefined;

  /**
   * 语言检测置信度
   */
  languageConfidence?: number;
}
