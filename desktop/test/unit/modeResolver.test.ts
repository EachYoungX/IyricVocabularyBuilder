import { win32 } from 'node:path';
import { describe, expect, it, vi } from 'vitest';
import { portableFlagPath, resolveDesktopMode } from '../../src/paths/modeResolver';

describe('resolveDesktopMode', () => {
  it('uses the isolated development mode for unpackaged Electron', () => {
    const fileExists = vi.fn(() => true);
    expect(resolveDesktopMode({
      isPackaged: false,
      executablePath: String.raw`C:\Apps\LyricVocabularyBuilder.exe`,
      fileExists,
      pathApi: win32,
    })).toBe('development');
    expect(fileExists).not.toHaveBeenCalled();
  });

  it('selects portable mode only when portable.flag exists beside the executable', () => {
    const executablePath = String.raw`D:\Portable Apps\LyricVocabularyBuilder.exe`;
    const expectedFlag = String.raw`D:\Portable Apps\portable.flag`;
    expect(portableFlagPath(executablePath, win32)).toBe(expectedFlag);
    expect(resolveDesktopMode({
      isPackaged: true,
      executablePath,
      fileExists: (candidate) => candidate === expectedFlag,
      pathApi: win32,
    })).toBe('portable');
  });

  it('defaults packaged builds without portable.flag to installed mode', () => {
    expect(resolveDesktopMode({
      isPackaged: true,
      executablePath: String.raw`C:\Program Files\LyricVocabularyBuilder\LyricVocabularyBuilder.exe`,
      fileExists: () => false,
      pathApi: win32,
    })).toBe('installed');
  });
});
