package com.github.yktakaha4.watsonmusic.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.yktakaha4.watsonmusic.mapper.TextTagMapper;
import com.github.yktakaha4.watsonmusic.model.HasTextTag;
import com.github.yktakaha4.watsonmusic.model.Text;
import com.github.yktakaha4.watsonmusic.model.TextTag;

@Service
public class TextTagService {
  private final TextTagMapper textTagMapper;

  @Autowired
  public TextTagService(TextTagMapper textTagMapper) {
    this.textTagMapper = textTagMapper;
  }

  @Transactional
  public void removeRelations(Text text) {
    textTagMapper.deleteByTextId(text.getTextId());
  }

  @Transactional
  public void addRelation(Text text, String textTagValue, Integer relevance) {
    if (getRelation(text, textTagValue).isPresent()) {
      return;
    }
    TextTag textTag = new TextTag();
    textTag.setTextId(text.getTextId());
    textTag.setTag(textTagValue);
    textTag.setRelevance(relevance);
    textTag.setCreatedAt(LocalDateTime.now());
    textTagMapper.insert(textTag);
  }

  @Transactional
  public void addRelation(Text text, HasTextTag hasTextTag, Integer relevance) {
    addRelation(text, hasTextTag.getTextTag(), relevance);
  }

  @Transactional(readOnly = true)
  public Optional<TextTag> getRelation(Text text, HasTextTag hasTextTag) {
    return getRelation(text, hasTextTag.getTextTag());
  }

  @Transactional(readOnly = true)
  public Optional<TextTag> getRelation(Text text, String textTag) {
    return Optional.ofNullable(textTagMapper.select(text.getTextId(), textTag));
  }

  @Transactional(readOnly = true)
  public List<TextTag> getRelationsByText(Text text) {
    return textTagMapper.selectByTextId(text.getTextId());
  }

  @Transactional(readOnly = true)
  public List<TextTag> getRelationsByTextTag(HasTextTag hasTextTag) {
    return textTagMapper.selectByTag(hasTextTag.getTextTag());
  }

  @Transactional
  public void removeAll() {
    textTagMapper.deleteAll();
  }

}
