import { readFile } from 'node:fs/promises';
import { resolve } from 'node:path';
import { describe, expect, it } from 'vitest';

const projectRoot = resolve(process.cwd());

describe('Windows installer configuration', () => {
  it('uses a stable assisted per-user NSIS identity and fixed install directory', async () => {
    const config = await readFile(resolve(projectRoot, 'electron-builder.yml'), 'utf8');

    expect(config).toContain('appId: com.each17.lyricvocabularybuilder');
    expect(config).toContain('productName: Lyric Vocabulary Builder');
    expect(config).toContain('executableName: LyricVocabularyBuilder');
    expect(config).toContain('oneClick: false');
    expect(config).toContain('perMachine: false');
    expect(config).toContain('allowElevation: false');
    expect(config).toContain('allowToChangeInstallationDirectory: false');
    expect(config).toContain('deleteAppDataOnUninstall: false');
  });

  it('keeps local data by default and deletes only the fixed application data root when selected', async () => {
    const script = await readFile(resolve(projectRoot, 'build', 'installer.nsh'), 'utf8');

    expect(script).toContain('Section /o');
    expect(script).toContain('将被永久删除');
    expect(script).toContain('RMDir /r "$LOCALAPPDATA\\LyricVocabularyBuilder"');
    expect(script).not.toContain('runtime.json');
    expect(script).not.toContain('externalPath');
  });
});
