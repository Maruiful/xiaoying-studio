package com.xiaoying.asset;

import static org.assertj.core.api.Assertions.assertThat;

import com.xiaoying.TestcontainersConfiguration;
import com.xiaoying.common.ErrorVO;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
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
 * 种子模板：demo-postcard 接受 JPG/PNG、上限 5MB；demo-polaroid 另外接受 WebP；demo-id-photo 只接受 JPG、上限 2MB。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.storage.dir=target/test-storage")
@Import(TestcontainersConfiguration.class)
class AssetUploadIT {

    private static final Path STORAGE = Path.of("target/test-storage");

    @Autowired
    TestRestTemplate rest;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    @DisplayName("上传 PNG 成功，返回 201，文件可被主人读取")
    void uploadsAndServesOwnAsset() {
        String uid = newUser();
        byte[] png = TestImages.png(1024);

        ResponseEntity<AssetVO> res =
                upload(uid, "demo-postcard", "beach.png", MediaType.IMAGE_PNG, png, AssetVO.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        AssetVO asset = res.getBody();
        assertThat(asset.templateId()).isEqualTo("demo-postcard");
        assertThat(asset.originalName()).isEqualTo("beach.png");
        assertThat(asset.mimeType()).isEqualTo("image/png");
        assertThat(asset.sizeBytes()).isEqualTo(1024);
        assertThat(asset.url()).isEqualTo("/api/files/assets/" + asset.id());

        ResponseEntity<byte[]> file = get(asset.url(), uid, byte[].class);
        assertThat(file.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(file.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_PNG);
        assertThat(file.getBody()).isEqualTo(png);
    }

    @Test
    @DisplayName("按文件头识别类型：文本文件改名为 .jpg 并声明 image/jpeg 也会被拒绝")
    void rejectsDisguisedFile() {
        String uid = newUser();
        byte[] text = "this is not an image".getBytes(StandardCharsets.UTF_8);

        ResponseEntity<ErrorVO> res =
                upload(uid, "demo-postcard", "fake.jpg", MediaType.IMAGE_JPEG, text, ErrorVO.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(res.getBody().code()).isEqualTo("ASSET_TYPE_NOT_ALLOWED");
        assertThat(res.getBody().message()).isEqualTo("只支持 JPG、PNG 格式的图片");
    }

    @Test
    @DisplayName("是否接受 WebP 由模板决定")
    void acceptedTypesDependOnTemplate() {
        String uid = newUser();
        byte[] webp = TestImages.webp(512);
        MediaType webpType = MediaType.parseMediaType("image/webp");

        var polaroid = upload(uid, "demo-polaroid", "a.webp", webpType, webp, AssetVO.class);
        var postcard = upload(uid, "demo-postcard", "a.webp", webpType, webp, ErrorVO.class);

        assertThat(polaroid.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(polaroid.getBody().mimeType()).isEqualTo("image/webp");
        assertThat(postcard.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    @DisplayName("超过模板的大小上限返回 413，并带上限字节数")
    void rejectsOversizedFile() {
        String uid = newUser();
        byte[] big = TestImages.jpeg(2 * 1024 * 1024 + 1);

        ResponseEntity<ErrorVO> res = upload(uid, "demo-id-photo", "big.jpg", MediaType.IMAGE_JPEG, big, ErrorVO.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(res.getBody().code()).isEqualTo("ASSET_TOO_LARGE");
        assertThat(res.getBody().message()).isEqualTo("图片不能超过 2MB");
        assertThat(res.getBody().details()).containsEntry("maxBytes", 2097152);
    }

    @Test
    @DisplayName("空文件返回 400 ASSET_EMPTY")
    void rejectsEmptyFile() {
        String uid = newUser();

        ResponseEntity<ErrorVO> res =
                upload(uid, "demo-postcard", "empty.png", MediaType.IMAGE_PNG, new byte[0], ErrorVO.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res.getBody().code()).isEqualTo("ASSET_EMPTY");
    }

    @Test
    @DisplayName("模板不存在返回 404，缺少 file 字段返回 400")
    void rejectsUnknownTemplateAndMissingFile() {
        String uid = newUser();

        var unknown = upload(uid, "nope", "a.png", MediaType.IMAGE_PNG, TestImages.png(64), ErrorVO.class);
        var missing = upload(uid, "demo-postcard", null, null, null, ErrorVO.class);

        assertThat(unknown.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(unknown.getBody().code()).isEqualTo("TEMPLATE_NOT_FOUND");
        assertThat(missing.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(missing.getBody().code()).isEqualTo("VALIDATION_FAILED");
    }

    @Test
    @DisplayName("被拒绝的上传不写数据库，也不留文件")
    void rejectedUploadLeavesNoTrace() {
        String uid = newUser();

        upload(
                uid,
                "demo-postcard",
                "fake.jpg",
                MediaType.IMAGE_JPEG,
                "x".repeat(64).getBytes(),
                ErrorVO.class);
        upload(uid, "demo-id-photo", "big.jpg", MediaType.IMAGE_JPEG, TestImages.jpeg(3 * 1024 * 1024), ErrorVO.class);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM asset WHERE user_id = ?", Integer.class, uid))
                .isZero();
        assertThat(Files.exists(STORAGE.resolve("assets").resolve(uid))).isFalse();
    }

    @Test
    @DisplayName("其他用户访问素材返回 404，和素材不存在一样")
    void othersCannotReadAsset() {
        String owner = newUser();
        String other = newUser();
        AssetVO asset = upload(owner, "demo-postcard", "a.png", MediaType.IMAGE_PNG, TestImages.png(64), AssetVO.class)
                .getBody();

        ResponseEntity<ErrorVO> res = get(asset.url(), other, ErrorVO.class);
        ResponseEntity<ErrorVO> missing = get("/api/files/assets/999999999", owner, ErrorVO.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(res.getBody().code()).isEqualTo("ASSET_NOT_FOUND");
        assertThat(missing.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ---- helpers ----

    private String newUser() {
        String setCookie =
                rest.getForEntity("/api/me", String.class).getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        return setCookie.substring("uid=".length(), setCookie.indexOf(';'));
    }

    private <T> ResponseEntity<T> upload(
            String uid, String templateId, String filename, MediaType type, byte[] content, Class<T> responseType) {
        var form = new LinkedMultiValueMap<String, Object>();
        form.add("templateId", templateId);
        if (content != null) {
            HttpHeaders partHeaders = new HttpHeaders();
            partHeaders.setContentType(type);
            form.add("file", new HttpEntity<>(namedResource(content, filename), partHeaders));
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.add(HttpHeaders.COOKIE, "uid=" + uid);
        return rest.exchange("/api/assets", HttpMethod.POST, new HttpEntity<>(form, headers), responseType);
    }

    private <T> ResponseEntity<T> get(String path, String uid, Class<T> type) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, "uid=" + uid);
        return rest.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), type);
    }

    private static ByteArrayResource namedResource(byte[] content, String filename) {
        return new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
    }
}
