package com.github.yktakaha4.watsonmusic.api.endpoint;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.github.yktakaha4.watsonmusic.api.dj.DiscJockey;
import com.github.yktakaha4.watsonmusic.api.dj.JockingTrack;
import com.github.yktakaha4.watsonmusic.model.Artwork;
import com.github.yktakaha4.watsonmusic.model.Music;
import com.github.yktakaha4.watsonmusic.service.MusicService;

@Controller
@RequestMapping("/api/artworks")
public class Artworks {
  private final MusicService musicService;
  private final DiscJockey discJockey;

  public Artworks(MusicService musicService, DiscJockey discJockey) {
    this.musicService = musicService;
    this.discJockey = discJockey;
  }

  @RequestMapping(path = "/{musicId}/jpg", method = RequestMethod.GET, produces = MediaType.IMAGE_JPEG_VALUE)
  public @ResponseBody byte[] getJpeg(@PathVariable("musicId") Integer musicId) {
    return getArtwork(musicId, "image/jpeg");
  }

  @RequestMapping(path = "/{musicId}/png", method = RequestMethod.GET, produces = MediaType.IMAGE_PNG_VALUE)
  public @ResponseBody byte[] getPng(@PathVariable("musicId") Integer musicId) {
    return getArtwork(musicId, "image/png");
  }

  @RequestMapping(path = "/{musicId}/gif", method = RequestMethod.GET, produces = MediaType.IMAGE_GIF_VALUE)
  public @ResponseBody byte[] getGif(@PathVariable("musicId") Integer musicId) {
    return getArtwork(musicId, "image/gif");
  }

  private byte[] getArtwork(Integer musicId, String mime) {
    List<Artwork> artworks = Collections.emptyList();
    Optional<JockingTrack> maybeJockingTrack = discJockey.getJockingTrack();
    if (maybeJockingTrack.isPresent()) {
      JockingTrack jockingTrack = maybeJockingTrack.get();
      if (jockingTrack.getMusic().getMusicId().equals(musicId)) {
        artworks = jockingTrack.getMusic().getArtworks();
      }
    } else {
      Optional<Music> maybeMusic = musicService.getByMusicId(musicId);
      if (maybeMusic.isPresent()) {
        artworks = maybeMusic.get().getArtworks();
      }
    }

    if (!artworks.isEmpty()) {
      Artwork artwork = artworks.get(0);
      if (StringUtils.equalsIgnoreCase(artwork.getMimetype(), mime)) {
        return artwork.getImage();
      }
    }
    return null;
  }

}
