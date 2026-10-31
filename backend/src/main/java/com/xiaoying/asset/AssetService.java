package com.xiaoying.asset;

import com.xiaoying.common.BizException;
import com.xiaoying.common.ErrorCode;
import com.xiaoying.template.TemplateEntity;
import com.xiaoying.template.TemplateService;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssetService {

    private static final int MAX_NAME_LENGTH = 255;

    private final TemplateService templateService;
    private final AssetMapper assetMapper;
    private final FileStorage fileStorage;
    private final Clock clock;

    /**
     * 按模板要求校验并保存一张图片。校验顺序：模板存在 → 非空 → 大小 → 真实类型。
     *
     * <p>不加事务：只有一条 INSERT，文件写入是 IO，不能放进事务。INSERT 失败时删除已写入的文件。
     */
    public AssetVO upload(String userId, String templateId, MultipartFile file) {
        TemplateEntity template = templateService.requireTemplate(templateId);

        if (file.isEmpty()) {
            throw new BizException(ErrorCode.ASSET_EMPTY);
        }
        if (file.getSize() > template.getMaxFileSizeBytes()) {
            throw new BizException(
                    ErrorCode.ASSET_TOO_LARGE,
                    "图片不能超过 " + formatSize(template.getMaxFileSizeBytes()),
                    Map.of("maxBytes", template.getMaxFileSizeBytes()));
        }
        ImageType type = detectType(file)
                .filter(t -> template.getAcceptedMimeTypes().contains(t.mimeType()))
                .orElseThrow(() -> new BizException(
                        ErrorCode.ASSET_TYPE_NOT_ALLOWED,
                        "只支持 " + formatTypes(template.getAcceptedMimeTypes()) + " 格式的图片",
                        Map.of("acceptedMimeTypes", template.getAcceptedMimeTypes())));

        String storagePath = "assets/%s/%s.%s".formatted(userId, UUID.randomUUID(), type.extension());
        try (InputStream in = file.getInputStream()) {
            fileStorage.save(storagePath, in);
        } catch (IOException e) {
            throw new UncheckedIOException("保存素材失败", e);
        }

        AssetEntity asset = new AssetEntity();
        asset.setUserId(userId);
        asset.setTemplateId(templateId);
        asset.setOriginalName(sanitizeName(file.getOriginalFilename()));
        asset.setMimeType(type.mimeType());
        asset.setSizeBytes(file.getSize());
        asset.setStoragePath(storagePath);
        asset.setCreatedAt(Instant.now(clock).truncatedTo(ChronoUnit.MILLIS));
        try {
            assetMapper.insert(asset);
        } catch (RuntimeException e) {
            fileStorage.deleteQuietly(storagePath);
            throw e;
        }
        return AssetVO.from(asset);
    }

    /** 读取当前用户自己的素材文件。素材不存在或属于其他用户时都返回 404。 */
    public AssetFile loadOwned(String userId, long assetId) {
        AssetEntity asset = assetMapper.findOwned(assetId, userId);
        if (asset == null) {
            throw new BizException(ErrorCode.ASSET_NOT_FOUND);
        }
        if (!fileStorage.exists(asset.getStoragePath())) {
            log.warn("素材记录存在但文件缺失: assetId={}", assetId);
            throw new BizException(ErrorCode.ASSET_NOT_FOUND);
        }
        return new AssetFile(fileStorage.load(asset.getStoragePath()), asset.getMimeType());
    }

    private static Optional<ImageType> detectType(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            return ImageType.detect(in.readNBytes(ImageType.HEADER_LENGTH));
        } catch (IOException e) {
            throw new UncheckedIOException("读取上传文件失败", e);
        }
    }

    /** 只保留文件名本身，去掉客户端可能带上的路径。 */
    static String sanitizeName(String original) {
        if (original == null || original.isBlank()) {
            return "image";
        }
        String name = original.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).strip();
        if (name.isEmpty()) {
            return "image";
        }
        return name.length() > MAX_NAME_LENGTH ? name.substring(0, MAX_NAME_LENGTH) : name;
    }

    private static String formatSize(long bytes) {
        long mb = 1024 * 1024;
        return bytes % mb == 0 ? (bytes / mb) + "MB" : (bytes / 1024) + "KB";
    }

    private static String formatTypes(List<String> mimeTypes) {
        return String.join(
                "、",
                mimeTypes.stream()
                        .map(m -> switch (m) {
                            case "image/jpeg" -> "JPG";
                            case "image/png" -> "PNG";
                            case "image/webp" -> "WebP";
                            default -> m;
                        })
                        .toList());
    }
}
