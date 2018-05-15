package com.github.yktakaha4.watsonmusic.batch.task;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.github.yktakaha4.watsonmusic.model.Artwork;
import com.github.yktakaha4.watsonmusic.model.VisualRecognitionResult;
import com.github.yktakaha4.watsonmusic.service.VisualRecognitionService;

@Controller
@StepScope
public class ArtworkToVisualRecognitionResultProcessor
    implements ItemProcessor<Artwork, Pair<Artwork, VisualRecognitionResult>> {
  private final VisualRecognitionService visualRecognitionService;

  @Autowired
  public ArtworkToVisualRecognitionResultProcessor(VisualRecognitionService visualRecognitionService) {
    this.visualRecognitionService = visualRecognitionService;
  }

  @Override
  public Pair<Artwork, VisualRecognitionResult> process(Artwork artwork) throws Exception {
    return visualRecognitionService.getVisualRecognitionResult(artwork.getImage(), artwork.getMimetype())
        .map((visualRecognitionResult) -> {
          return Pair.of(artwork, visualRecognitionResult);
        }).orElse(null);
  }
}
