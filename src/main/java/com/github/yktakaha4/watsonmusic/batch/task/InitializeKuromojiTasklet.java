package com.github.yktakaha4.watsonmusic.batch.task;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.github.yktakaha4.watsonmusic.model.WikiPage;
import com.github.yktakaha4.watsonmusic.service.WikipediaService;
import com.github.yktakaha4.watsonmusic.util.PathUtils;
import com.github.yktakaha4.watsonmusic.util.kuromoji.Kuromoji;
import com.github.yktakaha4.watsonmusic.util.kuromoji.KuromojiResult;
import com.github.yktakaha4.watsonmusic.util.kuromoji.Userdic;
import com.orangesignal.csv.Csv;
import com.orangesignal.csv.CsvConfig;
import com.orangesignal.csv.handlers.ColumnNameMapListHandler;
import com.orangesignal.csv.manager.CsvEntityManager;

@Controller
@StepScope
public class InitializeKuromojiTasklet implements Tasklet {
  private final WikipediaService wikipediaService;
  private final PathUtils pathUtils;
  private final Kuromoji kuromoji;
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Autowired
  public InitializeKuromojiTasklet(WikipediaService wikipediaService, PathUtils pathUtils, Kuromoji kuromoji) {
    this.wikipediaService = wikipediaService;
    this.pathUtils = pathUtils;
    this.kuromoji = kuromoji;
  }

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
    initializeUserDictionary();
    initializeStopWordsDictionary();

    return RepeatStatus.FINISHED;
  }

  private void initializeUserDictionary() throws Exception {
    final String charset = StandardCharsets.UTF_8.name();
    Kuromoji plainKuromoji = new Kuromoji();

    CsvConfig csvConfig = new CsvConfig();
    csvConfig.setIgnoreEmptyLines(true);
    csvConfig.setVariableColumns(false);
    CsvEntityManager csvEntityManager = new CsvEntityManager(csvConfig);

    Path sourcesPath = pathUtils.fromBatchProps(bp -> bp.getUserDictionarySourcesPath());
    List<Map<String, String>> userDicSoruces = Csv.load(sourcesPath.toFile(), charset, csvConfig,
        new ColumnNameMapListHandler());

    Path userDicPath = pathUtils.fromBatchProps(bp -> bp.getUserDictionaryFilePath());
    Map<String, Userdic> userDicRows = new HashMap<>();
    csvEntityManager.load(Userdic.class).from(userDicPath.toFile(), charset).forEach((userdic) -> {
      userDicRows.put(userdic.term, userdic);
    });
    logger.info("initial userdic rows: " + userDicRows.values().size());

    for (Map<String, String> userDicSourcesRow : userDicSoruces) {
      String title = userDicSourcesRow.get("wikipage_title");
      String partOfSpeech = userDicSourcesRow.get("part_of_speech");
      if (StringUtils.isBlank(title)) {
        continue;
      }

      logger.info("userdic source: " + title);

      // Wikipediaの記事が分割されないように
      Optional<WikiPage> maybeWikipage = wikipediaService.getWikiPage(title);
      if (maybeWikipage.isPresent()) {
        WikiPage wikiPage = maybeWikipage.get();

        for (String linkTitle : wikiPage.getLinkTitles().stream().map(lt -> lt.getLeft())
            .collect(Collectors.toList())) {
          if (userDicRows.containsKey(linkTitle)) {
            // 語の二重登録はしない
            continue;
          }

          // 読みを取得する
          List<KuromojiResult> kuromojiResults = plainKuromoji.analyze(linkTitle);

          if (kuromojiResults.size() == 1) {
            // 既に一語として解釈される語は登録しない
            continue;
          }

          Userdic userdicRow = new Userdic();
          userdicRow.term = linkTitle;
          userdicRow.tokenizedTerms = kuromojiResults.stream().map(kr -> kr.getCharTerm())
              .collect(Collectors.joining(" "))
              .replaceAll("\\s+", " ").trim();
          userdicRow.reading = kuromojiResults.stream()
              .map(kr -> StringUtils.defaultIfBlank(kr.getReading(), kr.getCharTerm())).collect(Collectors.joining(" "))
              .replaceAll("\\s+", " ").trim();
          userdicRow.partOfSpeech = partOfSpeech;

          if (userdicRow.tokenizedTerms.split("\\s+").length != userdicRow.reading.split("\\s+").length) {
            // 形態素解析後の語数と読みの語数が異なる場合、語をそのまま読みとして登録
            userdicRow.tokenizedTerms = linkTitle;
            userdicRow.reading = linkTitle;
          }

          userDicRows.put(linkTitle, userdicRow);
        }
      }
    }

    List<Userdic> userDic = new ArrayList<>(userDicRows.values().stream().filter((userDicRow) -> {
      // FIXME 半角スペースを含むユーザー辞書定義語が形態素解析時に半角スペースの個数分文字切れする...
      return !userDicRow.term.contains(" ");
    }).collect(Collectors.toList()));
    userDic.sort((l, r) -> {
      return l.term.compareTo(r.term);
    });
    logger.info("userdic rows: " + userDic.size());

    csvEntityManager.save(userDic, Userdic.class).to(userDicPath.toFile(), charset);

    kuromoji.loadUserDictionary(userDicPath);
    logger.info("loaded user dictionary: " + userDicPath.getFileName());
  }

  private void initializeStopWordsDictionary() throws Exception {
    Path stopWordsPath = pathUtils.fromBatchProps(bp -> bp.getStopWordsDictionaryFilePath());
    kuromoji.loadStopWordsDictionary(stopWordsPath, true);
    logger.info("loaded stopwords: " + stopWordsPath.getFileName());
  }

}
