package com.github.yktakaha4.watsonmusic.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.yktakaha4.watsonmusic.mapper.TextLinkMapper;
import com.github.yktakaha4.watsonmusic.model.Text;
import com.github.yktakaha4.watsonmusic.model.TextLink;

@Service
public class TextLinkService {
  private final TextLinkMapper textLinkMapper;

  @Autowired
  public TextLinkService(TextLinkMapper textLinkMapper) {
    this.textLinkMapper = textLinkMapper;
  }

  @Transactional(readOnly = true)
  public List<TextLink> getTextLinkByTextId(Integer textId) {
    return textLinkMapper.selectByTextId(textId);
  }

  @Transactional(readOnly = true)
  public List<TextLink> getTextLinkByTitle(String title) {
    return textLinkMapper.selectByTitle(title);

  }

  @Transactional(readOnly = true)
  public void fillTextLinks(Text text) {
    text.setTextLinks(textLinkMapper.selectByTextId(text.getTextId()));
  }

  @Transactional
  public void removeAll() {
    textLinkMapper.deleteAll();
  }

}
