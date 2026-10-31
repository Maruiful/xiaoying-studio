package com.xiaoying.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.xiaoying.TestcontainersConfiguration;
import com.xiaoying.asset.AssetVO;
import com.xiaoying.asset.TestImages;
import com.xiaoying.common.ErrorVO;
import com.xiaoying.simulation.SimulationVO;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.LinkedMultiValueMap;

/**
 * 端到端验证任务的受理与后台执行。使用真实 MySQL 和真实线程池，模拟耗时设为几十到几百毫秒。
 *
 * <p>模拟配置是全局状态，每个测试先设置自己需要的配置，结束后恢复为全部 0 耗时、不失败。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "app.storage.dir=target/test-storage",
            "app.dev-api.enabled=true",
            "app.simulation.check-assets-ms=0",
            "app.simulation.generate-ms=0",
            "app.simulation.organize-result-ms=0",
            "app.executor.core-size=4",
            "app.executor.max-size=4"
        })
@Import(TestcontainersConfiguration.class)
class TaskFlowIT {

    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    @Autowired
    TestRestTemplate rest;

    @Autowired
    JdbcTemplate jdbc;

    @AfterEach
    void resetSimulation() {
        simulate(0, 0, 0, "NONE");
    }

    // ---------------- 受理 ----------------

    @Test
    @DisplayName("提交接口立即返回 202 和任务 ID，不等待执行结束")
    void createReturnsImmediately() {
        simulate(0, 2000, 0, "NONE");
        String uid = newUser();
        List<Long> assets = uploadAssets(uid, "demo-postcard", 2);

        long start = System.nanoTime();
        ResponseEntity<CreateTaskVO> res = createTask(uid, "demo-postcard", assets, CreateTaskVO.class);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(elapsedMs).as("提交耗时").isLessThan(1500);
        TaskVO task = getTask(uid, res.getBody().taskId());
        assertThat(task.status()).isIn(TaskStatus.PENDING, TaskStatus.RUNNING);
        assertThat(task.resultUrl()).isNull();
    }

    // ---------------- 成功路径 ----------------

    @Test
    @DisplayName("三步按顺序执行后成功，记录结果地址，结果图可被主人访问")
    void succeedsThroughAllStages() {
        String uid = newUser();
        List<Long> assets = uploadAssets(uid, "demo-postcard", 2);

        long taskId = createTask(uid, "demo-postcard", assets, CreateTaskVO.class)
                .getBody()
                .taskId();
        TaskVO done = awaitTerminal(uid, taskId);

        assertThat(done.status()).isEqualTo(TaskStatus.SUCCEEDED);
        assertThat(done.stage()).isEqualTo(TaskStage.ORGANIZE_RESULT);
        assertThat(done.templateTitle()).isEqualTo("旅行明信片");
        assertThat(done.resultUrl()).isEqualTo("/api/files/results/" + taskId);
        assertThat(done.failReason()).isNull();
        assertThat(done.startedAt()).isNotNull();
        assertThat(done.finishedAt()).isAfterOrEqualTo(done.startedAt());

        // 每一步都有独立的执行记录，按顺序排列，且后一步开始不早于前一步结束
        List<Map<String, Object>> steps = steps(taskId);
        assertThat(steps)
                .extracting(s -> s.get("stage"))
                .containsExactly("CHECK_ASSETS", "GENERATE", "ORGANIZE_RESULT");
        assertThat(steps).extracting(s -> s.get("status")).containsOnly("SUCCEEDED");
        for (int i = 1; i < steps.size(); i++) {
            Instant prevEnd = toInstant(steps.get(i - 1).get("finished_at"));
            Instant start = toInstant(steps.get(i).get("started_at"));
            assertThat(start).isAfterOrEqualTo(prevEnd);
        }

        ResponseEntity<String> image = get(done.resultUrl(), uid, String.class);
        assertThat(image.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(image.getHeaders().getContentType()).isEqualTo(MediaType.valueOf("image/svg+xml"));
        assertThat(image.getBody()).startsWith("<svg");
    }

    @Test
    @DisplayName("执行中能观察到 RUNNING 和当前阶段")
    void exposesCurrentStageWhileRunning() {
        simulate(0, 1500, 0, "NONE");
        String uid = newUser();
        long taskId = createTask(
                        uid, "demo-retro-poster", uploadAssets(uid, "demo-retro-poster", 1), CreateTaskVO.class)
                .getBody()
                .taskId();

        await().atMost(TIMEOUT).untilAsserted(() -> {
            TaskVO t = getTask(uid, taskId);
            assertThat(t.status()).isEqualTo(TaskStatus.RUNNING);
            assertThat(t.stage()).isEqualTo(TaskStage.GENERATE);
        });
        assertThat(awaitTerminal(uid, taskId).status()).isEqualTo(TaskStatus.SUCCEEDED);
    }

    // ---------------- 失败路径 ----------------

    @Test
    @DisplayName("指定阶段失败：记录失败阶段和原因，后续步骤没有执行记录，也没有结果")
    void failsAtConfiguredStageAndStops() {
        for (String failStage : List.of("CHECK_ASSETS", "GENERATE", "ORGANIZE_RESULT")) {
            simulate(0, 0, 0, failStage);
            String uid = newUser();
            long taskId = createTask(
                            uid, "demo-retro-poster", uploadAssets(uid, "demo-retro-poster", 1), CreateTaskVO.class)
                    .getBody()
                    .taskId();

            TaskVO done = awaitTerminal(uid, taskId);

            TaskStage expected = TaskStage.valueOf(failStage);
            assertThat(done.status()).as(failStage).isEqualTo(TaskStatus.FAILED);
            assertThat(done.stage()).as(failStage).isEqualTo(expected);
            assertThat(done.failReason()).as(failStage).isEqualTo(expected.label() + "失败（模拟失败）");
            assertThat(done.resultUrl()).as(failStage).isNull();

            List<Map<String, Object>> steps = steps(taskId);
            assertThat(steps).as(failStage).hasSize(expected.ordinal() + 1);
            Map<String, Object> last = steps.get(steps.size() - 1);
            assertThat(last.get("stage")).isEqualTo(failStage);
            assertThat(last.get("status")).isEqualTo("FAILED");
            assertThat(last.get("message")).isEqualTo(done.failReason());

            assertThat(get("/api/files/results/" + taskId, uid, ErrorVO.class).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Test
    @DisplayName("模拟配置在提交时固定：提交后修改配置不影响已提交的任务")
    void usesSnapshotTakenAtSubmission() {
        simulate(0, 800, 0, "NONE");
        String uid = newUser();
        long taskId = createTask(
                        uid, "demo-retro-poster", uploadAssets(uid, "demo-retro-poster", 1), CreateTaskVO.class)
                .getBody()
                .taskId();

        simulate(0, 0, 0, "GENERATE");

        assertThat(awaitTerminal(uid, taskId).status()).isEqualTo(TaskStatus.SUCCEEDED);
    }

    @Test
    @DisplayName("提交后素材文件被删除时，素材检查步骤失败")
    void checkAssetsDetectsMissingFile() {
        simulate(300, 0, 0, "NONE");
        String uid = newUser();
        List<Long> assets = uploadAssets(uid, "demo-retro-poster", 1);
        String path = jdbc.queryForObject("SELECT storage_path FROM asset WHERE id = ?", String.class, assets.get(0));
        // 检查发生在模拟耗时之前，所以先删文件再提交
        java.nio.file.Path file = java.nio.file.Path.of("target/test-storage").resolve(path);
        assertThat(file.toFile().delete()).isTrue();

        long taskId = createTask(uid, "demo-retro-poster", assets, CreateTaskVO.class)
                .getBody()
                .taskId();
        TaskVO done = awaitTerminal(uid, taskId);

        assertThat(done.status()).isEqualTo(TaskStatus.FAILED);
        assertThat(done.stage()).isEqualTo(TaskStage.CHECK_ASSETS);
        assertThat(done.failReason()).startsWith("素材文件缺失");
    }

    // ---------------- 查询只读 / 终态不可变 ----------------

    @Test
    @DisplayName("查询不推动任务：执行期间频繁查询，各阶段的耗时不会被缩短")
    void queriesDoNotAdvanceTask() {
        simulate(0, 1000, 0, "NONE");
        String uid = newUser();
        long taskId = createTask(
                        uid, "demo-retro-poster", uploadAssets(uid, "demo-retro-poster", 1), CreateTaskVO.class)
                .getBody()
                .taskId();

        for (int i = 0; i < 20; i++) {
            getTask(uid, taskId);
            listTasks(uid);
        }
        awaitTerminal(uid, taskId);

        Map<String, Object> generate = steps(taskId).get(1);
        long ms = Duration.between(toInstant(generate.get("started_at")), toInstant(generate.get("finished_at")))
                .toMillis();
        assertThat(ms).as("GENERATE 步骤耗时").isGreaterThanOrEqualTo(950);
    }

    @Test
    @DisplayName("完成后的任务再次查询不会变回处理中，状态更新也不会覆盖终态")
    void terminalStateIsStable() {
        String uid = newUser();
        long taskId = createTask(
                        uid, "demo-retro-poster", uploadAssets(uid, "demo-retro-poster", 1), CreateTaskVO.class)
                .getBody()
                .taskId();
        TaskVO done = awaitTerminal(uid, taskId);

        for (int i = 0; i < 5; i++) {
            assertThat(getTask(uid, taskId)).isEqualTo(done);
        }

        // 直接调用状态更新 SQL：带前置状态条件，对终态任务不生效
        Integer updated = jdbc.update(
                "UPDATE task SET status = 'RUNNING', stage = 'GENERATE' WHERE id = ? AND status IN ('PENDING','RUNNING')",
                taskId);
        assertThat(updated).isZero();
        assertThat(getTask(uid, taskId)).isEqualTo(done);
    }

    // ---------------- 多任务 ----------------

    @Test
    @DisplayName("同时提交多个任务，各自的状态、阶段和结果互不干扰")
    void concurrentTasksAreIndependent() throws Exception {
        simulate(0, 300, 0, "NONE");
        String uid = newUser();
        List<List<Long>> assetSets = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            assetSets.add(uploadAssets(uid, "demo-retro-poster", 1));
        }

        var pool = Executors.newFixedThreadPool(5);
        List<Callable<Long>> submits = assetSets.stream()
                .<Callable<Long>>map(a -> () -> createTask(uid, "demo-retro-poster", a, CreateTaskVO.class)
                        .getBody()
                        .taskId())
                .toList();
        List<Long> ids = new ArrayList<>();
        for (var f : pool.invokeAll(submits)) {
            ids.add(f.get());
        }
        pool.shutdown();

        assertThat(ids).doesNotHaveDuplicates();
        for (long id : ids) {
            TaskVO done = awaitTerminal(uid, id);
            assertThat(done.status()).isEqualTo(TaskStatus.SUCCEEDED);
            assertThat(done.resultUrl()).isEqualTo("/api/files/results/" + id);
            assertThat(steps(id)).hasSize(3);
        }
        assertThat(listTasks(uid).total()).isEqualTo(5);
    }

    // ---------------- 校验与隔离 ----------------

    @Test
    @DisplayName("提交时校验素材数量、重复、归属和所属模板")
    void validatesSubmission() {
        String owner = newUser();
        String other = newUser();
        List<Long> postcardAssets = uploadAssets(owner, "demo-postcard", 2);
        List<Long> retroAsset = uploadAssets(owner, "demo-retro-poster", 1);

        var tooFew = createTask(owner, "demo-postcard", postcardAssets.subList(0, 1), ErrorVO.class);
        var duplicate = createTask(
                owner, "demo-postcard", List.of(postcardAssets.get(0), postcardAssets.get(0)), ErrorVO.class);
        var notMine = createTask(other, "demo-postcard", postcardAssets, ErrorVO.class);
        var wrongTemplate =
                createTask(owner, "demo-postcard", List.of(postcardAssets.get(0), retroAsset.get(0)), ErrorVO.class);
        var noTemplate = createTask(owner, "nope", postcardAssets, ErrorVO.class);
        var empty = createTask(owner, "demo-postcard", List.of(), ErrorVO.class);

        assertThat(tooFew.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(tooFew.getBody().code()).isEqualTo("TASK_ASSET_COUNT_MISMATCH");
        assertThat(tooFew.getBody().message()).isEqualTo("该模板需要 2 张图片");
        assertThat(duplicate.getBody().code()).isEqualTo("TASK_ASSET_INVALID");
        assertThat(notMine.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(notMine.getBody().code()).isEqualTo("TASK_ASSET_INVALID");
        assertThat(wrongTemplate.getBody().message()).isEqualTo("素材不属于该模板");
        assertThat(noTemplate.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(empty.getBody().code()).isEqualTo("VALIDATION_FAILED");

        // 校验失败不创建任务
        assertThat(listTasks(owner).total()).isZero();
        assertThat(listTasks(other).total()).isZero();
    }

    @Test
    @DisplayName("其他用户查不到任务和结果图，历史列表按用户隔离并按时间倒序")
    void tasksAreIsolatedPerUser() {
        String a = newUser();
        String b = newUser();
        long first = createTask(a, "demo-retro-poster", uploadAssets(a, "demo-retro-poster", 1), CreateTaskVO.class)
                .getBody()
                .taskId();
        long second = createTask(a, "demo-retro-poster", uploadAssets(a, "demo-retro-poster", 1), CreateTaskVO.class)
                .getBody()
                .taskId();
        awaitTerminal(a, first);
        awaitTerminal(a, second);

        assertThat(get("/api/tasks/" + first, b, ErrorVO.class).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(get("/api/files/results/" + first, b, ErrorVO.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(listTasks(b).items()).isEmpty();
        assertThat(listTasks(a).items()).extracting(TaskVO::id).containsExactly(second, first);
    }

    @Test
    @DisplayName("开发接口：查看与修改模拟配置，非法值返回 400")
    void simulationDevApi() {
        SimulationVO updated = rest.exchange(
                        "/api/dev/simulation",
                        HttpMethod.PUT,
                        json(Map.of("generateMs", 1234, "failStage", "generate")),
                        SimulationVO.class)
                .getBody();
        var invalid =
                rest.exchange("/api/dev/simulation", HttpMethod.PUT, json(Map.of("failStage", "LATER")), ErrorVO.class);
        var negative =
                rest.exchange("/api/dev/simulation", HttpMethod.PUT, json(Map.of("checkAssetsMs", -1)), ErrorVO.class);

        assertThat(updated.generateMs()).isEqualTo(1234);
        assertThat(updated.failStage()).isEqualTo("GENERATE");
        assertThat(rest.getForObject("/api/dev/simulation", SimulationVO.class)).isEqualTo(updated);
        assertThat(invalid.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(negative.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("启动清理：上次进程留下的 PENDING / RUNNING 任务标为失败，已完成的任务不受影响")
    void startupCleanerFailsUnfinishedTasks() {
        String uid = newUser();
        long done = createTask(uid, "demo-retro-poster", uploadAssets(uid, "demo-retro-poster", 1), CreateTaskVO.class)
                .getBody()
                .taskId();
        TaskVO doneBefore = awaitTerminal(uid, done);
        // 模拟上次进程崩溃时留下的两个任务：一个排队中，一个执行到第二步
        long pending = insertOrphan(uid, "PENDING", null);
        long running = insertOrphan(uid, "RUNNING", "GENERATE");
        jdbc.update(
                "INSERT INTO task_step (task_id, stage, status, started_at) VALUES (?, 'GENERATE', 'RUNNING', UTC_TIMESTAMP(3))",
                running);

        cleaner.failUnfinishedTasks();

        TaskVO p = getTask(uid, pending);
        TaskVO r = getTask(uid, running);
        assertThat(p.status()).isEqualTo(TaskStatus.FAILED);
        assertThat(p.failReason()).isEqualTo(UnfinishedTaskCleaner.RESTART_REASON);
        assertThat(r.status()).isEqualTo(TaskStatus.FAILED);
        assertThat(r.stage()).isEqualTo(TaskStage.GENERATE);
        assertThat(steps(running).get(0).get("status")).isEqualTo("FAILED");
        assertThat(getTask(uid, done)).isEqualTo(doneBefore);
    }

    @Autowired
    UnfinishedTaskCleaner cleaner;

    private long insertOrphan(String uid, String status, String stage) {
        jdbc.update("""
                INSERT INTO task (user_id, template_id, status, stage, sim_config, created_at, updated_at)
                VALUES (?, 'demo-retro-poster', ?, ?, '{}', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3))
                """, uid, status, stage);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    // ---------------- helpers ----------------

    private void simulate(long checkMs, long generateMs, long organizeMs, String failStage) {
        var res = rest.exchange(
                "/api/dev/simulation",
                HttpMethod.PUT,
                json(Map.of(
                        "checkAssetsMs", checkMs,
                        "generateMs", generateMs,
                        "organizeResultMs", organizeMs,
                        "failStage", failStage)),
                SimulationVO.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private TaskVO awaitTerminal(String uid, long taskId) {
        return await().atMost(TIMEOUT)
                .pollInterval(Duration.ofMillis(50))
                .until(() -> getTask(uid, taskId), t -> t.status().isTerminal());
    }

    private String newUser() {
        String setCookie =
                rest.getForEntity("/api/me", String.class).getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        return setCookie.substring("uid=".length(), setCookie.indexOf(';'));
    }

    private List<Long> uploadAssets(String uid, String templateId, int count) {
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            var form = new LinkedMultiValueMap<String, Object>();
            form.add("templateId", templateId);
            HttpHeaders partHeaders = new HttpHeaders();
            partHeaders.setContentType(MediaType.IMAGE_JPEG);
            form.add("file", new HttpEntity<>(named(TestImages.jpeg(256), "p" + i + ".jpg"), partHeaders));
            HttpHeaders headers = cookie(uid);
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            var res = rest.exchange("/api/assets", HttpMethod.POST, new HttpEntity<>(form, headers), AssetVO.class);
            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            ids.add(res.getBody().id());
        }
        return ids;
    }

    private <T> ResponseEntity<T> createTask(String uid, String templateId, List<Long> assetIds, Class<T> type) {
        HttpHeaders headers = cookie(uid);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.exchange(
                "/api/tasks",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("templateId", templateId, "assetIds", assetIds), headers),
                type);
    }

    private TaskVO getTask(String uid, long taskId) {
        ResponseEntity<TaskVO> res = get("/api/tasks/" + taskId, uid, TaskVO.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        return res.getBody();
    }

    private com.xiaoying.common.PageVO<TaskVO> listTasks(String uid) {
        return rest.exchange(
                        "/api/tasks?page=1&size=50",
                        HttpMethod.GET,
                        new HttpEntity<>(cookie(uid)),
                        new ParameterizedTypeReference<com.xiaoying.common.PageVO<TaskVO>>() {})
                .getBody();
    }

    private <T> ResponseEntity<T> get(String path, String uid, Class<T> type) {
        return rest.exchange(path, HttpMethod.GET, new HttpEntity<>(cookie(uid)), type);
    }

    private List<Map<String, Object>> steps(long taskId) {
        return jdbc.queryForList(
                "SELECT stage, status, message, started_at, finished_at FROM task_step WHERE task_id = ? ORDER BY id",
                taskId);
    }

    private static HttpHeaders cookie(String uid) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, "uid=" + uid);
        return headers;
    }

    private static HttpEntity<Object> json(Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private static Instant toInstant(Object value) {
        if (value instanceof java.time.LocalDateTime ldt) {
            return ldt.toInstant(java.time.ZoneOffset.UTC);
        }
        if (value instanceof java.sql.Timestamp ts) {
            return ts.toInstant();
        }
        throw new IllegalArgumentException("无法转换的时间类型: " + value);
    }

    private static ByteArrayResource named(byte[] content, String filename) {
        return new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
    }
}
