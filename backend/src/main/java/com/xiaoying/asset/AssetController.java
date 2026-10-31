package com.xiaoying.asset;

import com.xiaoying.user.CurrentUser;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;

    /** 上传一张素材图片。multipart 字段：file、templateId。 */
    @PostMapping(path = "/api/assets", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AssetVO upload(CurrentUser user, @RequestParam String templateId, @RequestPart MultipartFile file) {
        return assetService.upload(user.id(), templateId, file);
    }

    /** 读取素材文件，只有素材的主人能访问。 */
    @GetMapping("/api/files/assets/{assetId}")
    public ResponseEntity<Resource> file(CurrentUser user, @PathVariable long assetId) {
        AssetFile file = assetService.loadOwned(user.id(), assetId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.mimeType()))
                // private：按用户区分的内容，不允许共享缓存
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate())
                .header("X-Content-Type-Options", "nosniff")
                .body(file.resource());
    }
}
