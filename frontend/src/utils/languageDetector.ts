/**
 * 英语歌词检测工具
 * 用于检测歌词是否为英语，支持智能识别和温柔提醒
 */

// 英语字符正则表达式（包含常见标点符号）
const ENGLISH_REGEX = /^[A-Za-z0-9\s.,!?'"\-–—()[\]{}:;]+$/;

/**
 * 检测文本是否为英语
 * @param text 要检测的文本
 * @returns 如果文本很可能是英语返回 true
 */
export function isLikelyEnglish(text: string): boolean {
  if (!text || text.length < 20) return true; // 太短不判断

  // 清理文本：去除多余空格和换行
  const clean = text.replace(/[\r\n\s]+/g, ' ').trim();
  if (clean.length === 0) return true;

  // 计算英文字符比例
  const englishRatio =
    clean.split('').filter((char) => ENGLISH_REGEX.test(char)).length / clean.length;

  return englishRatio > 0.85; // 85% 以上是英文字符就认为合格
}

/**
 * 检测歌词并返回检测结果
 * @param lyrics 歌词文本
 * @param t 可选的国际化翻译函数
 * @returns 检测结果对象
 */
export function detectLyricsLanguage(
  lyrics: string,
  t?: (key: string, params?: Record<string, unknown>) => string,
): {
  isEnglish: boolean;
  confidence: number;
  warning?: string | undefined;
  isNonEnglish: boolean;
} {
  if (!lyrics || lyrics.trim().length < 20) {
    return {
      isEnglish: true,
      isNonEnglish: false,
      confidence: 1.0,
      warning: lyrics ? undefined : t ? t('lyricsEmpty') : 'Lyrics are empty',
    };
  }

  const clean = lyrics.replace(/[\r\n\s]+/g, ' ').trim();
  const englishChars = clean.split('').filter((char) => ENGLISH_REGEX.test(char)).length;
  const confidence = englishChars / clean.length;
  const isEnglish = confidence > 0.85;

  return {
    isEnglish,
    isNonEnglish: !isEnglish,
    confidence,
    warning: !isEnglish
      ? t
        ? t('nonEnglishLyricsWarning')
        : 'Non-English lyrics detected, use with caution for vocabulary learning'
      : undefined,
  };
}
