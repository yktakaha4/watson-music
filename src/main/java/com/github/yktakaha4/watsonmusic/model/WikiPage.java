package com.github.yktakaha4.watsonmusic.model;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import lombok.Data;

@Data
public class WikiPage {
  private String title;
  private String content;
  private String raw;
  private List<Pair<String, LinkType>> linkTitles;
}
