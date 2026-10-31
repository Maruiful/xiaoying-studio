package com.xiaoying.asset;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ImageTypeTest {

    @Test
    @DisplayName("按文件头识别 JPG、PNG、WebP")
    void detectsByMagicBytes() {
        assertThat(ImageType.detect(TestImages.jpeg(16))).contains(ImageType.JPEG);
        assertThat(ImageType.detect(TestImages.png(16))).contains(ImageType.PNG);
        assertThat(ImageType.detect(TestImages.webp(16))).contains(ImageType.WEBP);
    }

    @Test
    @DisplayName("文本、GIF、过短的数据都识别不出")
    void rejectsOtherContent() {
        assertThat(ImageType.detect("hello, not an image".getBytes(StandardCharsets.UTF_8)))
                .isEmpty();
        assertThat(ImageType.detect("GIF89a......".getBytes(StandardCharsets.US_ASCII)))
                .isEmpty();
        assertThat(ImageType.detect(new byte[] {(byte) 0xFF, (byte) 0xD8})).isEmpty();
        assertThat(ImageType.detect(new byte[0])).isEmpty();
    }

    @Test
    @DisplayName("RIFF 开头但不是 WEBP 的文件（如 WAV）不算 WebP")
    void riffWithoutWebpMarkIsNotWebp() {
        byte[] wav = "RIFF\0\0\0\0WAVEfmt ".getBytes(StandardCharsets.US_ASCII);

        assertThat(ImageType.detect(wav)).isEmpty();
    }

    @Test
    @DisplayName("文件名去掉路径，空名称用默认值")
    void sanitizesOriginalName() {
        assertThat(AssetService.sanitizeName("C:\\Users\\me\\photo.jpg")).isEqualTo("photo.jpg");
        assertThat(AssetService.sanitizeName("../../etc/passwd")).isEqualTo("passwd");
        assertThat(AssetService.sanitizeName("  ")).isEqualTo("image");
        assertThat(AssetService.sanitizeName(null)).isEqualTo("image");
        assertThat(AssetService.sanitizeName("dir/")).isEqualTo("image");
    }
}
