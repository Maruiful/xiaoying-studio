package com.xiaoying.task;

import com.xiaoying.common.PageVO;
import com.xiaoying.user.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    /** 受理制作任务：返回 202 和任务 ID，不等待执行结束。 */
    @PostMapping("/api/tasks")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CreateTaskVO create(CurrentUser user, @Valid @RequestBody CreateTaskRequest request) {
        return taskService.create(user.id(), request);
    }

    /** 查询单个任务。只读。 */
    @GetMapping("/api/tasks/{id}")
    public ResponseEntity<TaskVO> get(CurrentUser user, @PathVariable long id) {
        // 状态会变化，禁止浏览器和中间层缓存
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(taskService.get(user.id(), id));
    }

    /** 当前用户的历史任务。只读。 */
    @GetMapping("/api/tasks")
    public ResponseEntity<PageVO<TaskVO>> list(
            CurrentUser user, @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(taskService.list(user.id(), page, size));
    }

    /** 成功任务的结果图，只有任务的主人能访问。 */
    @GetMapping("/api/files/results/{taskId}")
    public ResponseEntity<Resource> result(CurrentUser user, @PathVariable long taskId) {
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("image/svg+xml"))
                .cacheControl(CacheControl.noCache().cachePrivate())
                .header("X-Content-Type-Options", "nosniff")
                // 结果图是 SVG，禁止其中的脚本执行
                .header("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'; sandbox")
                .body(taskService.loadResult(user.id(), taskId));
    }
}
