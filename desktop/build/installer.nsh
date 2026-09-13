!macro customUnInstallSection
  Section /o "un.同时删除本地数据（不可恢复，请确认已备份）" SEC_DELETE_LOCAL_DATA
    IfSilent keepLocalData
    MessageBox MB_ICONEXCLAMATION|MB_YESNO|MB_DEFBUTTON2 \
      "勾选此项后，歌曲、个人词库、用户短语、设置以及应用管理的本地数据集将被永久删除。请确认不再需要这些数据，或已经创建并妥善保存完整备份。" \
      /SD IDNO IDYES deleteLocalData IDNO keepLocalData

    deleteLocalData:
      RMDir /r "$LOCALAPPDATA\LyricVocabularyBuilder"
      Goto localDataSectionDone

    keepLocalData:
      DetailPrint "保留 Lyric Vocabulary Builder 本地数据。"

    localDataSectionDone:
  SectionEnd
!macroend
