package com.github.yktakaha4.watsonmusic.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Component;

@Component
public class IdentifierUtils {
  public String toHash(byte[] data) {
    return DigestUtils.md5Hex(data).toUpperCase();

  }

  public String createFileHash(File file) throws IOException {
    return toHash(Files.readAllBytes(file.toPath()));

  }

  public String newTag() {
    return UUID.randomUUID().toString().toUpperCase();

  }

  public String identifier(String... strings) {
    final String delim = "@";
    return Stream.of(strings).map(s -> s.toUpperCase().replaceAll(delim, delim + delim))
        .collect(Collectors.joining(delim));
  }

}
