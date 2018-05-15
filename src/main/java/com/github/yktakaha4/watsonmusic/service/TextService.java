package com.github.yktakaha4.watsonmusic.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.yktakaha4.watsonmusic.mapper.TextLinkMapper;
import com.github.yktakaha4.watsonmusic.mapper.TextMapper;
import com.github.yktakaha4.watsonmusic.model.Source;
import com.github.yktakaha4.watsonmusic.model.Text;
import com.github.yktakaha4.watsonmusic.model.TextLink;
import com.github.yktakaha4.watsonmusic.model.WikiPage;

@Service
public class TextService {
  private final TextMapper textMapper;
  private final TextLinkMapper textLinkMapper;
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Autowired
  public TextService(TextMapper textMapper, TextLinkMapper textLinkMapper) {
    this.textMapper = textMapper;
    this.textLinkMapper = textLinkMapper;
  }

  @Transactional(readOnly = true)
  public List<Text> getTextsBySource(Source source) {
    return textMapper.selectBySource(source).stream().peek(this::fillTextLinks).collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public Optional<Text> getText(Source source, String title) {
    Optional<Text> maybeText = Optional.ofNullable(textMapper.selectBySourceAndTitle(source, title));
    maybeText.ifPresent(this::fillTextLinks);
    return maybeText;
  }

  public Optional<Text> getTextByTextId(Integer textId) {
    Optional<Text> maybeText = Optional.ofNullable(textMapper.selectByTextId(textId));
    maybeText.ifPresent(this::fillTextLinks);
    return maybeText;
  }

  @Transactional
  public void manage(Text text) {
    Optional<Text> maybeText = Optional
        .ofNullable(textMapper.selectBySourceAndTitle(text.getSource(), text.getTitle()));
    if (maybeText.isPresent()) {
      logger.debug("already managed text: " + text.getTitle());
    } else {
      textMapper.insert(text);
      Text newText = textMapper.selectBySourceAndTitle(text.getSource(), text.getTitle());

      newText.setTextLinks(Stream.iterate(0, i -> i + 1).limit(text.getTextLinks().size()).map((index) -> {
        TextLink newTextLink = new TextLink();
        newTextLink.setTextId(newText.getTextId());
        newTextLink.setSeq(index);
        newTextLink.setTitle(text.getTextLinks().get(index).getTitle());
        newTextLink.setLinkType(text.getTextLinks().get(index).getLinkType());
        return newTextLink;
      }).collect(Collectors.toList()));

      textLinkMapper.deleteByTextId(newText.getTextId());
      newText.getTextLinks().forEach(textLinkMapper::insert);
      logger.debug("insert text: " + text.getTitle());
    }
  }

  @Transactional
  public void removeAll() {
    textMapper.deleteAll();
  }

  public Text createText(WikiPage wikiPage) {
    Text text = new Text();
    text.setTitle(wikiPage.getTitle());
    text.setSource(Source.WIKIPEDIA);
    text.setContent(wikiPage.getContent());
    text.setRaw(wikiPage.getRaw());
    text.setTextLinks(wikiPage.getLinkTitles().stream().map((linkTitle) -> {
      TextLink textLink = new TextLink();
      textLink.setTitle(linkTitle.getLeft());
      textLink.setLinkType(linkTitle.getRight());
      return textLink;
    }).collect(Collectors.toList()));

    return text;
  }

  private void fillTextLinks(Text text) {
    text.setTextLinks(textLinkMapper.selectByTextId(text.getTextId()));
  }

}
