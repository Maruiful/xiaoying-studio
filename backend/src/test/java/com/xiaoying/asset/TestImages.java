package com.xiaoying.asset;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** 测试用的"图片"：只有正确的文件头，后面用 0 填充到指定大小。服务端只按文件头识别类型。 */
public final class TestImages {

    private TestImages() {}

    public static byte[] jpeg(int size) {
        return pad(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0}, size);
    }

    public static byte[] png(int size) {
        return pad(new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'}, size);
    }

    public static byte[] webp(int size) {
        return pad("RIFF\0\0\0\0WEBPVP8 ".getBytes(StandardCharsets.US_ASCII), size);
    }

    private static byte[] pad(byte[] header, int size) {
        return Arrays.copyOf(header, Math.max(size, header.length));
    }
}
