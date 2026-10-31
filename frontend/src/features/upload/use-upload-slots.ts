"use client";

import { useCallback, useEffect, useReducer, useRef } from "react";
import type { TemplateDetail } from "@/features/templates/types";
import { ApiError } from "@/lib/api-client";
import { uploadAsset } from "./api";
import { initSlots, previewUrlOf, slotsReducer } from "./slots";
import { validateFile } from "./validate";

type Template = Pick<
  TemplateDetail,
  "id" | "requiredImageCount" | "acceptedMimeTypes" | "maxFileSizeBytes"
>;

/** 管理一组上传位：发起和取消上传请求，创建和释放预览用的 object URL。 */
export function useUploadSlots(template: Template) {
  const [slots, dispatch] = useReducer(
    slotsReducer,
    template.requiredImageCount,
    initSlots,
  );
  const controllers = useRef(new Map<number, AbortController>());
  const nextUploadId = useRef(0);

  const abort = useCallback((index: number) => {
    controllers.current.get(index)?.abort();
    controllers.current.delete(index);
  }, []);

  const select = useCallback(
    (index: number, file: File) => {
      abort(index);
      const problem = validateFile(file, template);
      if (problem) {
        dispatch({ type: "rejected", index, file, message: problem });
        return;
      }

      const uploadId = ++nextUploadId.current;
      const controller = new AbortController();
      controllers.current.set(index, controller);
      dispatch({
        type: "started",
        index,
        file,
        previewUrl: URL.createObjectURL(file),
        uploadId,
      });

      uploadAsset(template.id, file, controller.signal)
        .then((asset) =>
          dispatch({ type: "succeeded", index, uploadId, assetId: asset.id }),
        )
        .catch((error: unknown) => {
          if (controller.signal.aborted) return;
          dispatch({ type: "failed", index, uploadId, ...describe(error) });
        })
        .finally(() => {
          if (controllers.current.get(index) === controller) {
            controllers.current.delete(index);
          }
        });
    },
    [abort, template],
  );

  const remove = useCallback(
    (index: number) => {
      abort(index);
      dispatch({ type: "removed", index });
    },
    [abort],
  );

  const reset = useCallback(() => {
    controllers.current.forEach((c) => c.abort());
    controllers.current.clear();
    dispatch({ type: "reset", count: template.requiredImageCount });
  }, [template.requiredImageCount]);

  // 释放不再使用的预览地址：对比前后两次的 URL 集合
  const usedUrls = useRef(new Set<string>());
  useEffect(() => {
    const current = new Set(
      slots.map(previewUrlOf).filter((u): u is string => u !== null),
    );
    for (const url of usedUrls.current) {
      if (!current.has(url)) URL.revokeObjectURL(url);
    }
    usedUrls.current = current;
  }, [slots]);

  // 卸载时取消所有请求并释放全部预览地址
  useEffect(() => {
    const pending = controllers.current;
    const urls = usedUrls;
    return () => {
      pending.forEach((c) => c.abort());
      pending.clear();
      urls.current.forEach((url) => URL.revokeObjectURL(url));
      urls.current = new Set();
    };
  }, []);

  return { slots, select, remove, reset };
}

function describe(error: unknown): { message: string; retryable: boolean } {
  if (error instanceof ApiError) {
    // 4xx 说明文件或参数本身被服务端拒绝，同一个文件重试也不会成功
    const clientError = error.status >= 400 && error.status < 500;
    return { message: error.message, retryable: !clientError };
  }
  if (error instanceof Error) {
    return { message: error.message, retryable: true };
  }
  return { message: "上传失败", retryable: true };
}
