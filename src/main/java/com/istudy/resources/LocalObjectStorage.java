package com.istudy.resources;

import java.io.*;
import java.nio.file.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LocalObjectStorage implements ObjectStorage {

  private final Path root;

  public LocalObjectStorage(
    @Value("${app.storage.local-root:data/private-assets}") String path
  ) {
    root = Path.of(path).toAbsolutePath().normalize();
  }

  private Path path(String key) throws IOException {
    Path result = root.resolve(key).normalize();
    if (!result.startsWith(root)) throw new IOException("Invalid object key");
    return result;
  }

  public InputStream read(String key) throws IOException {
    return Files.newInputStream(path(key));
  }

  public void write(String key, InputStream content) throws IOException {
    Path dest = path(key);
    Files.createDirectories(dest.getParent());
    Files.copy(content, dest, StandardCopyOption.REPLACE_EXISTING);
  }
}
