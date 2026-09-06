import {
  VocabularyStatus,
  type UserVocabulary,
} from 'src/services/api';

export type BackupVocabularyItem = {
  lemma: string;
  status?: VocabularyStatus | undefined;
  note?: string | null;
};

export function timestampForFilename() {
  return new Date().toISOString().replaceAll(':', '-').replace(/\.\d{3}Z$/, 'Z');
}

export function downloadTextFile(filename: string, content: string, type: string) {
  const blob = new Blob([content], { type });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  link.click();
  URL.revokeObjectURL(url);
}

export function vocabularyToCsv(words: UserVocabulary[]) {
  const headers = ['lemma', 'status', 'note'];
  const rows = words.map((word) => [
    word.lemma,
    word.status,
    word.note,
  ]);
  return [headers, ...rows].map((row) => row.map(csvCell).join(',')).join('\n');
}

export function vocabularyToAnkiTsv(
  words: UserVocabulary[],
  formatStatus: (status: VocabularyStatus) => string = (status) => status,
) {
  const rows = words.map((word) => [
    word.lemma,
    formatStatus(word.status),
    word.note ?? '',
  ].map(tsvCell).join('\t'));
  return rows.join('\n');
}

export function parseVocabularyText(content: string, fileName: string): BackupVocabularyItem[] {
  const delimiter = fileName.toLowerCase().endsWith('.tsv') ? '\t' : ',';
  const rows = content
    .replace(/\r\n/g, '\n')
    .replace(/\r/g, '\n')
    .split('\n')
    .map((line) => parseDelimitedLine(line, delimiter))
    .filter((row) => row.some((cell) => cell.trim()));
  if (rows.length === 0) return [];

  const header = rows[0]?.map((cell) => cell.trim().toLowerCase()) ?? [];
  const hasHeader = header.some((cell) => ['lemma', 'word', '单词', 'status', '状态'].includes(cell));
  const dataRows = hasHeader ? rows.slice(1) : rows;
  const lemmaIndex = hasHeader ? firstHeaderIndex(header, ['lemma', 'word', '单词']) : 0;
  const statusIndex = hasHeader ? firstHeaderIndex(header, ['status', '状态']) : 1;
  const noteIndex = hasHeader ? firstHeaderIndex(header, ['note', '备注']) : 2;
  const statuses = new Set<string>(Object.values(VocabularyStatus));

  return dataRows
    .map((row) => {
      const rawStatus = statusIndex >= 0 ? row[statusIndex]?.trim() : '';
      return {
        lemma: row[lemmaIndex]?.trim() ?? '',
        status: rawStatus && statuses.has(rawStatus) ? rawStatus as VocabularyStatus : undefined,
        note: noteIndex >= 0 ? row[noteIndex]?.trim() || null : null,
      };
    })
    .filter((word) => word.lemma);
}

function csvCell(value: string | number | boolean | null | undefined) {
  const text = value === null || value === undefined ? '' : String(value);
  return `"${text.replaceAll('"', '""')}"`;
}

function tsvCell(value: string | number | boolean | null | undefined) {
  return (value === null || value === undefined ? '' : String(value))
    .replace(/\r?\n/g, '<br>')
    .replace(/\t/g, ' ');
}

function parseDelimitedLine(line: string, delimiter: string) {
  if (delimiter === '\t') return line.split('\t').map((cell) => cell.replaceAll('<br>', '\n').trim());
  const cells: string[] = [];
  let current = '';
  let quoted = false;
  for (let index = 0; index < line.length; index += 1) {
    const char = line[index];
    const next = line[index + 1];
    if (char === '"' && next === '"') {
      current += '"';
      index += 1;
    } else if (char === '"') {
      quoted = !quoted;
    } else if (char === ',' && !quoted) {
      cells.push(current.trim());
      current = '';
    } else {
      current += char;
    }
  }
  cells.push(current.trim());
  return cells;
}

function firstHeaderIndex(header: string[], candidates: string[]) {
  const index = header.findIndex((cell) => candidates.includes(cell));
  return index >= 0 ? index : -1;
}
