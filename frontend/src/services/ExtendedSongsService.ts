import { OpenAPI } from './api/core/OpenAPI';
import { request as __request } from './api/core/request';
import type { CancelablePromise } from './api/core/CancelablePromise';
import type { ImportTaskResult } from './api/models/ImportTaskResult';
import type { Song } from './api/models/Song';
import type { SongUpdateRequest } from './api/models/SongUpdateRequest';
import { SongsService } from './api/services/SongsService';
import type { SongImportRequest } from './api/models/SongImportRequest';
import type { SongImportTaskResponse } from './api/models/SongImportTaskResponse';

export class ExtendedSongsService {
  /**
   * 异步批量导入歌曲（推荐方式）
   * @param requestBody
   * @returns any 导入任务已接受，正在后台处理
   * @throws ApiError
   */
  public static importSongsAsync(
    requestBody: SongImportRequest[],
  ): CancelablePromise<SongImportTaskResponse> {
    return SongsService.importSongsAsync(requestBody);
  }

  /**
   * 查询导入任务完整结果（包含失败详情）
   * @param taskId 任务ID
   * @returns ImportTaskResult 任务结果详情
   * @throws ApiError
   */
  public static getImportTaskResult(taskId: string): CancelablePromise<ImportTaskResult> {
    return __request(OpenAPI, {
      method: 'GET',
      url: '/api/songs/import/tasks/{taskId}',
      path: {
        taskId: taskId,
      },
    });
  }

  /**
   * 更新歌曲 / Update song
   * @param id 歌曲ID
   * @param requestBody 歌曲更新请求
   * @returns Song 更新后的歌曲信息
   * @throws ApiError
   */
  public static updateSong(id: number, requestBody: SongUpdateRequest): CancelablePromise<Song> {
    return __request(OpenAPI, {
      method: 'PUT',
      url: '/api/songs/{id}',
      path: {
        id: id,
      },
      body: requestBody,
    });
  }

  /**
   * 删除歌曲 / Delete song
   * @param id 歌曲ID
   * @returns void
   * @throws ApiError
   */
  public static deleteSong(id: number): CancelablePromise<void> {
    return __request(OpenAPI, {
      method: 'DELETE',
      url: '/api/songs/{id}',
      path: {
        id: id,
      },
    });
  }
}
