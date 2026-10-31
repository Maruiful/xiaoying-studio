import { describe, expect, it } from "vitest";
import { pickLatest } from "./progress";
import type { Task } from "./types";

function task(patch: Partial<Task>): Task {
  return {
    id: 1,
    templateId: "demo-postcard",
    templateTitle: "复古明信片",
    status: "PENDING",
    stage: null,
    resultUrl: null,
    failReason: null,
    createdAt: "2026-11-01T00:00:00Z",
    startedAt: null,
    finishedAt: null,
    ...patch,
  };
}

describe("pickLatest", () => {
  const pending = task({});
  const checking = task({ status: "RUNNING", stage: "CHECK_ASSETS" });
  const generating = task({ status: "RUNNING", stage: "GENERATE" });
  const succeeded = task({
    status: "SUCCEEDED",
    stage: "ORGANIZE_RESULT",
    resultUrl: "/api/files/results/1",
  });
  const failed = task({
    status: "FAILED",
    stage: "GENERATE",
    failReason: "模拟生成失败",
  });

  it("较旧的列表数据不会让已结束的任务退回处理中", () => {
    expect(pickLatest(succeeded, generating)).toBe(succeeded);
    expect(pickLatest(failed, pending)).toBe(failed);
  });

  it("较旧的数据不会让阶段倒退", () => {
    expect(pickLatest(generating, checking)).toBe(generating);
  });

  it("进度更靠后时采用新数据", () => {
    expect(pickLatest(pending, checking)).toBe(checking);
    expect(pickLatest(generating, succeeded)).toBe(succeeded);
    expect(pickLatest(generating, failed)).toBe(failed);
  });

  it("进度相同时采用新数据", () => {
    const fresher = task({
      status: "RUNNING",
      stage: "GENERATE",
      startedAt: "2026-11-01T00:00:01Z",
    });
    expect(pickLatest(generating, fresher)).toBe(fresher);
  });
});
