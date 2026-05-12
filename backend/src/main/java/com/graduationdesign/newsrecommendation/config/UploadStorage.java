package com.graduationdesign.newsrecommendation.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class UploadStorage {

    private final Path uploadRoot;

    public UploadStorage(@Value("${app.storage.upload-dir:uploads}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    public Path getUploadRoot() {
        return uploadRoot;
    }

    public Path getAvatarDirectory() {
        return uploadRoot.resolve("avatars");
    }

    public String getAvatarAccessPath(String filename) {
        return "/uploads/avatars/" + filename;
    }
}
