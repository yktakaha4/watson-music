package com.github.yktakaha4.watsonmusic.batch.task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.yktakaha4.watsonmusic.model.Document;
import com.github.yktakaha4.watsonmusic.model.DocumentContent;
import com.github.yktakaha4.watsonmusic.model.Music;
import com.github.yktakaha4.watsonmusic.service.DocumentService;
import com.github.yktakaha4.watsonmusic.service.TextService;
import com.github.yktakaha4.watsonmusic.util.IdentifierUtils;

@Controller
@StepScope
public class MusicToDocumentProcessor implements ItemProcessor<Music, Document> {
  private final DocumentService documentService;
  private final TextService textService;
  private final IdentifierUtils identifierUtils;

  private static final int DOCUMENT_MAX_LENGTH = 1000;

  @Autowired
  public MusicToDocumentProcessor(DocumentService documentService, TextService textService,
      IdentifierUtils identifierUtils) {
    this.documentService = documentService;
    this.textService = textService;
    this.identifierUtils = identifierUtils;
  }

  @Override
  public Document process(Music music) throws Exception {
    DocumentContent documentContent = new DocumentContent();
    documentContent.setMusicId(music.getMusicId());
    documentContent.setPath(music.getPath());
    documentContent.setEncoding(music.getEncoding());
    documentContent.setTrackLength(music.getTrackLength());

    Map<String, String> musicTags = new HashMap<>();
    music.getMusicTags().forEach((musicTag) -> {
      musicTags.put(musicTag.getName().toString(), musicTag.getValue());
    });
    documentContent.setMusicTags(musicTags);

    List<String> texts = new ArrayList<>();
    texts.addAll(musicTags.values().stream().sorted().distinct().collect(Collectors.toList()));
    texts.addAll(documentService.getDocumentSourcesByMusicId(music.getMusicId()).stream().map((documentSource) -> {
      return textService.getTextByTextId(documentSource.getTextId()).get();
    }).map((t) -> {
      return StringUtils.left(t.getContent(), DOCUMENT_MAX_LENGTH);
    }).sorted().distinct().collect(Collectors.toList()));
    documentContent.setText(String.join(" ", texts));

    Document document = new Document();
    document.setMusicId(music.getMusicId());
    String content = new ObjectMapper().writeValueAsString(documentContent);
    document.setContent(content);
    document.setContentHash(identifierUtils.toHash(content.getBytes()));
    return document;
  }

}
