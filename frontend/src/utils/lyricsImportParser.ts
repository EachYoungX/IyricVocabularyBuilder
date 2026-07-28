import type { ExtendedSongImportRequest, LyricImportSummary } from 'src/types/songImport';

type Translate = (key: string, params?: Record<string, unknown>) => string;
type LyricsProcessingSettings = {
  roleLabelHandling?: 'AUTO_HIDE' | 'AUTO_DELETE' | 'KEEP_VISIBLE' | 'CONFIRM_EACH_IMPORT';
  repeatedChorusHandling?: 'KEEP_ALL' | 'DEDUP_LEARNING_STATS';
  fillerWordHandling?: 'NOT_RECOMMENDED' | 'NORMAL' | 'EXCLUDE_STATS';
};

const SECTION_LABEL = /^\s*\[(?:verse|chorus|bridge|intro|outro|pre-chorus|hook|refrain|post-chorus|interlude|solo|spoken)(?:\s+\d+)?\]\s*$/i;
const SPEAKER_LABEL = /^[A-Z][A-Za-z.'-]*(?:\s+[A-Z][A-Za-z.'-]*){0,3}:\s*$/;
const PERFORMANCE_NOTE = /^\s*(?:guitar|piano|drum|bass|violin|sax|instrumental|solo|crowd|audience|spoken|whistle|clap|applause|laugh|music)(?:\s+.*)?\s*$/i;
const META_INFO = /^\s*(?:produced|written|lyrics|music|composed|performed|arranged|mixed|mastered|recorded)\s+(?:by\b|:).*/i;
const LRC_TIMESTAMP = /\[\d{1,2}:\d{2}(?:[.:]\d{1,3})?\]/g;
const SRT_INDEX = /^\d+$/;
const SRT_TIMING = /^\d{1,2}:\d{2}:\d{2},\d{3}\s+-->\s+\d{1,2}:\d{2}:\d{2},\d{3}/;
const TITLE_ARTIST_SEPARATOR = /\s-\s/;

export function parseImportFileContent(
  fileName: string,
  content: string,
  t: Translate,
  settings: LyricsProcessingSettings = {},
): ExtendedSongImportRequest[] {
  const lowerName = fileName.toLowerCase();
  const attachSource = (song: ExtendedSongImportRequest): ExtendedSongImportRequest => ({
    ...song,
    rawSourceContent: content,
    parsedLyricsContent: song.lyrics,
    sourceName: fileName,
  });
  if (lowerName.endsWith('.json')) return parseJson(content, t).map((song) => attachSource(applyLyricsProcessing(song, settings)));
  if (lowerName.endsWith('.lrc')) return [attachSource(applyLyricsProcessing(parseTimedText(fileName, content, 'LRC', t), settings))];
  if (lowerName.endsWith('.srt')) return [attachSource(applyLyricsProcessing(parseTimedText(fileName, content, 'SRT', t), settings))];
  if (lowerName.endsWith('.txt')) return [attachSource(applyLyricsProcessing(parsePlainText(fileName, content, t), settings))];
  if (lowerName.endsWith('.qrc')) throw new Error(t('encryptedQrcUnsupported'));
  throw new Error(t('unsupportedFileFormat'));
}

export function buildImportSummary(lyrics: string): LyricImportSummary {
  const lines = lyrics.replace(/\r\n/g, '\n').replace(/\r/g, '\n').split('\n');
  const summary: LyricImportSummary = {
    totalLines: lines.length,
    lyricLines: 0,
    sectionLabels: 0,
    speakerLabels: 0,
    performanceNotes: 0,
    metadataLines: 0,
    emptyLines: 0,
    hiddenLines: 0,
    unknownLines: 0,
  };

  for (const line of lines) {
    const normalized = normalizeLine(line);
    if (!normalized) {
      summary.emptyLines += 1;
    } else if (SECTION_LABEL.test(normalized)) {
      summary.sectionLabels += 1;
      summary.hiddenLines += 1;
    } else if (SPEAKER_LABEL.test(normalized)) {
      summary.speakerLabels += 1;
      summary.hiddenLines += 1;
    } else if (PERFORMANCE_NOTE.test(normalized)) {
      summary.performanceNotes += 1;
      summary.hiddenLines += 1;
    } else if (META_INFO.test(normalized)) {
      summary.metadataLines += 1;
      summary.hiddenLines += 1;
    } else if (/[A-Za-z]/.test(normalized)) {
      summary.lyricLines += 1;
    } else {
      summary.unknownLines += 1;
    }
  }
  return summary;
}

function parseJson(content: string, t: Translate): ExtendedSongImportRequest[] {
  const data: unknown = JSON.parse(content);
  if (!Array.isArray(data)) throw new Error(t('invalidJsonFormat'));

  return data
    .map((item): ExtendedSongImportRequest | null => {
      if (!item || typeof item !== 'object') return null;
      const record = item as { title?: string; artist?: string; lyrics?: string };
      const song: ExtendedSongImportRequest = {
        title: record.title || '',
        artist: record.artist || '',
        lyrics: record.lyrics || '',
        sourceFormat: 'JSON',
      };
      if (song.title && song.artist && song.lyrics) return withSummary(song);
      return null;
    })
    .filter((song): song is ExtendedSongImportRequest => song !== null);
}

function parsePlainText(fileName: string, content: string, t: Translate): ExtendedSongImportRequest {
  let lines = content.split(/\r?\n/).map((line) => line.trim());
  const titleArtist = inferTitleArtist(fileName, lines[0] || '', '.txt');
  if (titleArtist.fromFirstLine) lines = lines.slice(1);

  const lyrics = trimTrailingEmptyLines(stripLeadingMetadata(lines)).join('\n');
  if (!titleArtist.title) throw new Error(t('couldNotDetermineSongTitle'));
  if (!lyrics) throw new Error(t('noValidLyricContentFound'));

  return withSummary({
    title: titleArtist.title,
    artist: titleArtist.artist,
    lyrics,
    sourceFormat: 'TXT',
  });
}

function parseTimedText(
  fileName: string,
  content: string,
  format: 'LRC' | 'SRT',
  t: Translate,
): ExtendedSongImportRequest {
  const titleArtist = inferTitleArtist(fileName, '', format === 'LRC' ? '.lrc' : '.srt');
  const lyrics = format === 'LRC' ? parseLrcLyrics(content) : parseSrtLyrics(content);
  if (!titleArtist.title) throw new Error(t('couldNotDetermineSongTitle'));
  if (!lyrics) throw new Error(t('noValidLyricContentFound'));

  return withSummary({
    title: titleArtist.title,
    artist: titleArtist.artist,
    lyrics,
    sourceFormat: format,
  });
}

function parseLrcLyrics(content: string): string {
  return trimTrailingEmptyLines(
    content
      .split(/\r?\n/)
      .map((line) => line.replace(/^\s*\[(?:ti|ar|al|by|offset):[^\]]*]\s*/i, '').replace(LRC_TIMESTAMP, '').trim())
      .filter((line) => line.length > 0),
  ).join('\n');
}

function parseSrtLyrics(content: string): string {
  return trimTrailingEmptyLines(
    content
      .split(/\r?\n/)
      .map((line) => line.trim())
      .filter((line) => line && !SRT_INDEX.test(line) && !SRT_TIMING.test(line))
      .map((line) => line.replace(/<[^>]+>/g, '').trim())
      .filter((line) => line.length > 0),
  ).join('\n');
}

function inferTitleArtist(fileName: string, firstLine: string, extension: string) {
  if (TITLE_ARTIST_SEPARATOR.test(firstLine)) {
    const parts = firstLine.split(TITLE_ARTIST_SEPARATOR);
    return {
      title: parts[0]?.trim() || '',
      artist: parts.slice(1).join(' - ').trim() || 'Unknown Artist',
      fromFirstLine: true,
    };
  }

  const nameWithoutExt = fileName.replace(new RegExp(`${escapeRegex(extension)}$`, 'i'), '');
  const qqMusic = inferQqMusicFileName(nameWithoutExt);
  if (qqMusic) return qqMusic;

  if (TITLE_ARTIST_SEPARATOR.test(nameWithoutExt)) {
    const parts = nameWithoutExt.split(TITLE_ARTIST_SEPARATOR);
    return {
      title: parts[0]?.trim() || '',
      artist: parts.slice(1).join(' - ').trim() || 'Unknown Artist',
      fromFirstLine: false,
    };
  }

  return {
    title: nameWithoutExt.trim(),
    artist: 'Unknown Artist',
    fromFirstLine: false,
  };
}

function inferQqMusicFileName(nameWithoutExt: string) {
  const parts = nameWithoutExt.split(TITLE_ARTIST_SEPARATOR).map((part) => part.trim()).filter(Boolean);
  const durationIndex = parts.findIndex((part) => /^\d{2,4}$/.test(part));
  if (durationIndex >= 2) {
    return {
      title: parts[durationIndex - 1] || '',
      artist: parts.slice(0, durationIndex - 1).join(' - ') || 'Unknown Artist',
      fromFirstLine: false,
    };
  }
  return null;
}

function stripLeadingMetadata(lines: string[]) {
  const cleanLyricsLines: string[] = [];
  let lyricsStarted = false;
  for (const line of lines) {
    if (lyricsStarted) {
      cleanLyricsLines.push(line);
    } else {
      if (!line) continue;
      if (isMetadataLine(line)) continue;
      lyricsStarted = true;
      cleanLyricsLines.push(line);
    }
  }
  return cleanLyricsLines;
}

function isMetadataLine(line: string): boolean {
  const lower = line.toLowerCase();
  const roles = [
    'lyrics',
    'composed',
    'arranged',
    'produced',
    'mixed',
    'mastered',
    'vocal',
    'guitar',
    'bass',
    'drum',
    'strings',
    'piano',
    'keyboard',
    'recording',
    'engineering',
    'edit',
    'background',
    'harmony',
    '作词',
    '作曲',
    '编曲',
    '制作',
    '吉他',
    '贝斯',
    '鼓',
    '和声',
    '录音',
    '混音',
  ];
  const rolePattern = new RegExp(`^\\s*(${roles.join('|')})`, 'i');
  if (rolePattern.test(lower)) return true;
  return line.length < 60 && (lower.includes(' by ') || /[:：]/.test(line));
}

function normalizeLine(line: string) {
  return line.replace(LRC_TIMESTAMP, '').replace(/\s+/g, ' ').trim();
}

function applyLyricsProcessing(song: ExtendedSongImportRequest, settings: LyricsProcessingSettings) {
  const processedLines = processLyricLines(song.lyrics, settings);
  const lyrics = processedLines.join('\n');
  return {
    ...song,
    lyrics,
    importSummary: buildImportSummary(lyrics),
  };
}

function processLyricLines(lyrics: string, settings: LyricsProcessingSettings) {
  let lines = lyrics.replace(/\r\n/g, '\n').replace(/\r/g, '\n').split('\n');

  if (settings.roleLabelHandling === 'AUTO_DELETE') {
    lines = lines.filter((line) => {
      const normalized = normalizeLine(line);
      return !normalized
        || (!SECTION_LABEL.test(normalized)
          && !SPEAKER_LABEL.test(normalized)
          && !PERFORMANCE_NOTE.test(normalized)
          && !META_INFO.test(normalized));
    });
  }

  if (settings.fillerWordHandling === 'EXCLUDE_STATS') {
    lines = lines.filter((line) => !isFillerOnlyLine(normalizeLine(line)));
  }

  if (settings.repeatedChorusHandling === 'DEDUP_LEARNING_STATS') {
    lines = dedupeRepeatedLyricLines(lines);
  }

  return trimTrailingEmptyLines(lines);
}

function dedupeRepeatedLyricLines(lines: string[]) {
  const seen = new Set<string>();
  return lines.filter((line) => {
    const normalized = normalizeLine(line).toLowerCase();
    if (!normalized || SECTION_LABEL.test(normalized)) return true;
    if (seen.has(normalized)) return false;
    seen.add(normalized);
    return true;
  });
}

function isFillerOnlyLine(line: string) {
  if (!line) return false;
  const fillerPattern = /^(?:oh+|ah+|uh+|um+|mm+|hmm+|na|la|da|yeah|yep|hey|woo+|ooh+|whoa|ha|haha|啦|啊|呀|喔|哦|嗯|呐|哒|哈)[\s,.'’!?-]*$/i;
  return fillerPattern.test(line);
}

function trimTrailingEmptyLines(lines: string[]) {
  const result = [...lines];
  while (result.length > 0 && !result[result.length - 1]) result.pop();
  return result;
}

function withSummary(song: ExtendedSongImportRequest): ExtendedSongImportRequest {
  return {
    ...song,
    importSummary: buildImportSummary(song.lyrics),
  };
}

function escapeRegex(value: string) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}
