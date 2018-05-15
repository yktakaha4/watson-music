package com.github.yktakaha4.watsonmusic.util;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.github.yktakaha4.watsonmusic.BatchProperties;
import com.github.yktakaha4.watsonmusic.WebProperties;

@Component
public class PathUtils {
  private final BatchProperties batchProperties;
  private final WebProperties webProperties;
  private final Path basePath;

  @Autowired
  public PathUtils(BatchProperties batchProperties, WebProperties webProperties) {
    this.batchProperties = batchProperties;
    this.webProperties = webProperties;
    this.basePath = Paths.get(batchProperties.getMusicfilePath());
  }

  public Path fromBasePath(Path path) {
    return basePath.relativize(path).normalize();
  }

  public Path resolveFromBasePath(Path path) {
    return basePath.resolve(path.toString()).normalize();
  }

  public Path fromClassPath(String pathString) throws IOException {
    return Paths.get(new ClassPathResource(pathString).getURI());
  }

  public Path fromBatchProps(Function<BatchProperties, String> function) throws IOException {
    return fromClassPath(function.apply(batchProperties));
  }

  public Path fromWebProps(Function<WebProperties, String> function) throws IOException {
    return fromClassPath(function.apply(webProperties));
  }

}
