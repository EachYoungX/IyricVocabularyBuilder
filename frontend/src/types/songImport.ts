/**
 * 扩展的歌曲导入请求类型，支持语言检测标记
 */
import type { SongImportRequest } from 'src/services/api';

export interface LyricImportSummary {
  totalLines: number;
  lyricLines: number;
  sectionLabels: number;
  speakerLabels: number;
  performanceNotes: number;
  metadataLines: number;
  emptyLines: number;
  hiddenLines: number;
  unknownLines: number;
}

export interface ExtendedSongImportRequest extends SongImportRequest {
  /**
   * 用户导入的原始文件内容，或手动输入说明。
   */
  rawSourceContent?: string;

  /**
   * 系统从源内容解析出的初始歌词正文。
   */
  parsedLyricsContent?: string;

  /**
   * 导入来源文件名或手动来源名。
   */
  sourceName?: string;

  /**
   * 导入来源格式
   */
  sourceFormat?: 'JSON' | 'TXT' | 'LRC' | 'SRT' | 'MANUAL';

  /**
   * 导入前清洗/结构识别摘要
   */
  importSummary?: LyricImportSummary;

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
