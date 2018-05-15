package com.github.yktakaha4.watsonmusic.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.yktakaha4.watsonmusic.mapper.ArtworkMapper;
import com.github.yktakaha4.watsonmusic.model.Artwork;

@Service
public class ArtworkService {
  private final ArtworkMapper artworkMapper;

  @Autowired
  public ArtworkService(ArtworkMapper artworkMapper) {
    this.artworkMapper = artworkMapper;
  }

  public List<Artwork> getAllArtworks() {
    return artworkMapper.selectAll();
  }

}
