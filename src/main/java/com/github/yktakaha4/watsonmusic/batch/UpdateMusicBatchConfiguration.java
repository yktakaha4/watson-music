package com.github.yktakaha4.watsonmusic.batch;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import com.github.yktakaha4.watsonmusic.BatchProperties;
import com.github.yktakaha4.watsonmusic.batch.task.ArtworkReader;
import com.github.yktakaha4.watsonmusic.batch.task.ArtworkToVisualRecognitionResultProcessor;
import com.github.yktakaha4.watsonmusic.batch.task.DocumentMarkShouldDeleteTasklet;
import com.github.yktakaha4.watsonmusic.batch.task.DocumentPublisher;
import com.github.yktakaha4.watsonmusic.batch.task.DocumentReader;
import com.github.yktakaha4.watsonmusic.batch.task.DocumentWriter;
import com.github.yktakaha4.watsonmusic.batch.task.InitializeKuromojiTasklet;
import com.github.yktakaha4.watsonmusic.batch.task.InitializeTableTasklet;
import com.github.yktakaha4.watsonmusic.batch.task.LivingMusicReader;
import com.github.yktakaha4.watsonmusic.batch.task.LyricsTextWriter;
import com.github.yktakaha4.watsonmusic.batch.task.MusicFileReader;
import com.github.yktakaha4.watsonmusic.batch.task.MusicFileToMusicProcessor;
import com.github.yktakaha4.watsonmusic.batch.task.MusicTagValueReader;
import com.github.yktakaha4.watsonmusic.batch.task.MusicToDocumentProcessor;
import com.github.yktakaha4.watsonmusic.batch.task.MusicToLyricsProcessor;
import com.github.yktakaha4.watsonmusic.batch.task.MusicWriter;
import com.github.yktakaha4.watsonmusic.batch.task.RelatedWikiPageProcessor;
import com.github.yktakaha4.watsonmusic.batch.task.RelatedWikiPageWriter;
import com.github.yktakaha4.watsonmusic.batch.task.VisualRecognitionResultTextWriter;
import com.github.yktakaha4.watsonmusic.batch.task.WikiPageTextReader;
import com.github.yktakaha4.watsonmusic.batch.task.WikiPageTextWriter;
import com.github.yktakaha4.watsonmusic.model.Artwork;
import com.github.yktakaha4.watsonmusic.model.Document;
import com.github.yktakaha4.watsonmusic.model.Lyrics;
import com.github.yktakaha4.watsonmusic.model.Music;
import com.github.yktakaha4.watsonmusic.model.Text;
import com.github.yktakaha4.watsonmusic.model.VisualRecognitionResult;
import com.github.yktakaha4.watsonmusic.model.WikiPage;

@Configuration
@EnableBatchProcessing
public class UpdateMusicBatchConfiguration {
  @Autowired
  private BatchProperties batchProperties;
  @Autowired
  private JobBuilderFactory jobBuilderFactory;
  @Autowired
  private StepBuilderFactory stepBuilderFactory;

  @Autowired
  private InitializeTableTasklet initializeTableTasklet;
  @Autowired
  private InitializeKuromojiTasklet initializeKuromojiTasklet;

  @Autowired
  private MusicFileReader musicFileReader;
  @Autowired
  private MusicFileToMusicProcessor musicFileToMusicProcessor;
  @Autowired
  private MusicWriter musicWriter;

  @Autowired
  private MusicTagValueReader musicTagValueReader;
  @Autowired
  private WikiPageTextWriter wikiPageTextWriter;

  @Autowired
  private WikiPageTextReader wikiPageTextReader;
  @Autowired
  private RelatedWikiPageProcessor relatedWikiPageProcessor;
  @Autowired
  private RelatedWikiPageWriter relatedWikiPageWriter;

  @Autowired
  private ArtworkReader artworkReader;
  @Autowired
  private ArtworkToVisualRecognitionResultProcessor artworkToVisualRecognitionResultProcessor;
  @Autowired
  private VisualRecognitionResultTextWriter visualRecognitionResultTextWriter;

  @Autowired
  private LivingMusicReader livingMusicReader;
  @Autowired
  private MusicToLyricsProcessor musicToLyricsProcessor;
  @Autowired
  private LyricsTextWriter lyricsTextWriter;

  @Autowired
  private MusicToDocumentProcessor musicToDocumentProcessor;
  @Autowired
  private DocumentWriter documentWriter;

  @Autowired
  private DocumentMarkShouldDeleteTasklet documentMarkShouldDeleteTasklet;

  @Autowired
  private DocumentReader documentReader;
  @Autowired
  private DocumentPublisher documentPublisher;

  public Job main(LocalDateTime localDateTime) {
    return jobBuilderFactory.get("updateMusic-" + localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        .incrementer(new RunIdIncrementer())
        .listener(new JobLogger())
        .flow(initializeTable())
        .next(initializeKuromoji())
        .next(transformFilesToMusic())
        .next(enrichMusicTagsByWikipedia())
        .next(enrichTextsByWikipedia())
        .next(enrichArtworksByVisualRecognition())
        .next(enrichSongsByLyrics())
        .next(transformMusicToDocuments())
        .next(documentMarkShouldDelete())
        .next(publishDocumentsToDiscovery())
        .end()
        .build();
  }

  public Step initializeTable() {
    return stepBuilderFactory.get("initializeTable")
        .tasklet(initializeTableTasklet)
        .build();
  }

  public Step initializeKuromoji() {
    return stepBuilderFactory.get("initializeKuromoji")
        .tasklet(initializeKuromojiTasklet)
        .build();
  }

  public Step transformFilesToMusic() {
    return stepBuilderFactory.get("transformFilesToMusic")
        .<File, Music> chunk(batchProperties.getBigChunk())
        .reader(musicFileReader)
        .processor(musicFileToMusicProcessor)
        .writer(musicWriter)
        .build();
  }

  public Step enrichMusicTagsByWikipedia() {
    return stepBuilderFactory.get("enrichMusicTagsByWikipedia")
        .<String, String> chunk(batchProperties.getMediumChunk())
        .reader(musicTagValueReader)
        .writer(wikiPageTextWriter)
        .build();
  }

  public Step enrichTextsByWikipedia() {
    return stepBuilderFactory.get("enrichTextsByWikipedia")
        .<Text, Pair<Text, List<WikiPage>>> chunk(batchProperties.getMediumChunk())
        .reader(wikiPageTextReader)
        .processor(relatedWikiPageProcessor)
        .writer(relatedWikiPageWriter)
        .build();
  }

  public Step enrichArtworksByVisualRecognition() {
    return stepBuilderFactory.get("enrichArtworksByVisualRecognition")
        .<Artwork, Pair<Artwork, VisualRecognitionResult>> chunk(batchProperties.getMediumChunk())
        .reader(artworkReader)
        .processor(artworkToVisualRecognitionResultProcessor)
        .writer(visualRecognitionResultTextWriter)
        .build();
  }

  public Step enrichSongsByLyrics() {
    return stepBuilderFactory.get("enrichSongsByLyrics")
        .<Music, Pair<Music, Lyrics>> chunk(batchProperties.getMediumChunk())
        .reader(livingMusicReader)
        .processor(musicToLyricsProcessor)
        .writer(lyricsTextWriter)
        .build();
  }

  public Step transformMusicToDocuments() {
    return stepBuilderFactory.get("transformMusicToDocuments")
        .<Music, Document> chunk(batchProperties.getMediumChunk())
        .reader(livingMusicReader)
        .processor(musicToDocumentProcessor)
        .writer(documentWriter)
        .build();
  }

  public Step documentMarkShouldDelete() {
    return stepBuilderFactory.get("documentMarkShouldDelete")
        .tasklet(documentMarkShouldDeleteTasklet)
        .build();
  }

  public Step publishDocumentsToDiscovery() {
    return stepBuilderFactory.get("publishDocumentsToDiscovery")
        .<Document, Document> chunk(1)
        .reader(documentReader)
        .writer(documentPublisher)
        .build();
  }

}
