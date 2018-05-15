package com.github.yktakaha4.watsonmusic.model;

public enum Source {
  WIKIPEDIA, VISUAL_RECOGNITION, LYRICS, LYRICS_ARTIST_SEARCH, LYRICS_NAMES_BY_ARTIST, DISCOVERY;

  public Source getIntervalSource() {
    if (this.equals(LYRICS_ARTIST_SEARCH) || this.equals(LYRICS_NAMES_BY_ARTIST)) {
      return LYRICS;
    } else {
      return this;
    }
  }
}
