package com.xiaoying.asset;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

/**
 * 支持的图片类型，按文件头字节（magic number）识别真实类型，不信任客户端声明的 Content-Type 和扩展名。
 *
 * <p>只检查文件头，不解码整张图片。
 */
public enum ImageType {
    JPEG("image/jpeg", "jpg"),
    PNG("image/png", "png"),
    WEBP("image/webp", "webp");

    /** 识别所需的最少字节数（WebP 需要前 12 字节）。 */
    public static final int HEADER_LENGTH = 12;

    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
    private static final byte[] RIFF = "RIFF".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] WEBP_MARK = "WEBP".getBytes(StandardCharsets.US_ASCII);

    private final String mimeType;
    private final String extension;

    ImageType(String mimeType, String extension) {
        this.mimeType = mimeType;
        this.extension = extension;
    }

    public String mimeType() {
        return mimeType;
    }

    public String extension() {
        return extension;
    }

    public static Optional<ImageType> detect(byte[] header) {
        if (startsWith(header, 0, JPEG_MAGIC)) {
            return Optional.of(JPEG);
        }
        if (startsWith(header, 0, PNG_MAGIC)) {
            return Optional.of(PNG);
        }
        if (startsWith(header, 0, RIFF) && startsWith(header, 8, WEBP_MARK)) {
            return Optional.of(WEBP);
        }
        return Optional.empty();
    }

    private static boolean startsWith(byte[] data, int offset, byte[] prefix) {
        return data.length >= offset + prefix.length
                && Arrays.equals(data, offset, offset + prefix.length, prefix, 0, prefix.length);
    }
}
