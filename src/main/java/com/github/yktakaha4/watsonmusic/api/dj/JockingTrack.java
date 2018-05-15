package com.github.yktakaha4.watsonmusic.api.dj;

import java.time.LocalDateTime;

import com.github.yktakaha4.watsonmusic.model.Music;
import com.github.yktakaha4.watsonmusic.model.Track;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class JockingTrack {
  private Track track;
  private Music music;
  private LocalDateTime playingAt;
}
