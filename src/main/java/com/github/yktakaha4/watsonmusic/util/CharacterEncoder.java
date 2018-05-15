package com.github.yktakaha4.watsonmusic.util;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.search.spell.StringDistance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.yktakaha4.watsonmusic.BatchProperties;

@Component
public class CharacterEncoder {
  private final List<Pair<Charset, Charset>> charsetPairs;

  @Autowired
  public CharacterEncoder(BatchProperties batchProperties, PathUtils pathUtils) throws IOException {
    List<Charset> charsets = Arrays.asList(batchProperties.getCharacterEncoderCharsets().split(",")).stream()
        .map(Charset::forName)
        .collect(Collectors.toList());
    this.charsetPairs = charsets.stream().flatMap(decode -> {
      return charsets.stream().map(encode -> {
        return Pair.of(decode, encode);
      });
    }).collect(Collectors.toList());
  }

  public Pair<Charset, Charset> detect(Collection<String> sources, Collection<String> hints) {
    StringDistance stringDistance = new NormalizedStringDistance();
    return charsetPairs.stream().map(pair -> {
      Double distance = hints.stream().flatMapToDouble(hint -> {
        return sources.stream().mapToDouble(source -> {
          String converted = new String(source.getBytes(pair.getLeft()), pair.getRight());
          return stringDistance.getDistance(converted, hint);
        });
      }).max().orElse(0);
      return Pair.of(pair, distance);
    }).sorted((l, r) -> {
      return r.getValue().compareTo(l.getValue());
    }).findFirst().map(pair -> pair.getKey()).orElse(Pair.of(StandardCharsets.UTF_8, StandardCharsets.UTF_8));
  }

}
