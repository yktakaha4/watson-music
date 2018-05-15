package com.github.yktakaha4.watsonmusic.model;

import java.util.List;

import lombok.Data;

@Data
public class LyricsPage {
  private String artistName;
  private String path;
  private List<LyricsPageLink> lyricsPageLinks;
}
