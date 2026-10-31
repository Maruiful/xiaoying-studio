package com.xiaoying.asset;

import com.xiaoying.config.StorageProperties;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * 本地目录存储。调用方只传相对路径（如 assets/{userId}/{uuid}.png），由这里拼到根目录下，
 * 并拒绝跳出根目录的路径。
 */
@Slf4j
@Component
public class FileStorage {

    private final Path root;

    public FileStorage(StorageProperties props) {
        this.root = props.dir().toAbsolutePath().normalize();
    }

    /** 先写临时文件再移动到目标位置，失败时不会留下写了一半的文件。 */
    public void save(String relativePath, InputStream in) throws IOException {
        Path target = resolve(relativePath);
        Files.createDirectories(target.getParent());
        Path tmp = Files.createTempFile(target.getParent(), ".upload-", ".tmp");
        try {
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
            Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            Files.deleteIfExists(tmp);
            throw e;
        }
    }

    public boolean exists(String relativePath) {
        return Files.isRegularFile(resolve(relativePath));
    }

    public Resource load(String relativePath) {
        return new FileSystemResource(resolve(relativePath));
    }

    public void deleteQuietly(String relativePath) {
        try {
            Files.deleteIfExists(resolve(relativePath));
        } catch (IOException e) {
            log.warn("删除文件失败: {}", relativePath, e);
        }
    }

    private Path resolve(String relativePath) {
        Path path = root.resolve(relativePath).normalize();
        if (!path.startsWith(root)) {
            throw new IllegalArgumentException("路径超出存储目录: " + relativePath);
        }
        return path;
    }
}
