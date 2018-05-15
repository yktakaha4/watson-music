package com.github.yktakaha4.watsonmusic.util.kuromoji;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.codelibs.neologd.ipadic.lucene.analysis.ja.JapaneseAnalyzer;
import org.codelibs.neologd.ipadic.lucene.analysis.ja.JapaneseTokenizer.Mode;
import org.codelibs.neologd.ipadic.lucene.analysis.ja.dict.UserDictionary;
import org.codelibs.neologd.ipadic.lucene.analysis.ja.tokenattributes.PartOfSpeechAttribute;
import org.codelibs.neologd.ipadic.lucene.analysis.ja.tokenattributes.ReadingAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.github.yktakaha4.watsonmusic.ApplicationException;

@Component
public class Kuromoji {
  private UserDictionary userDictionary = null;
  private CharArraySet stopWords = JapaneseAnalyzer.getDefaultStopSet();
  private Set<String> stopTags = JapaneseAnalyzer.getDefaultStopTags();
  private Mode mode = Mode.NORMAL;

  private Logger logger = LoggerFactory.getLogger(this.getClass());

  public UserDictionary getUserDictionary() {
    return userDictionary;
  }

  public void setUserDictionary(UserDictionary userDictionary) {
    this.userDictionary = userDictionary;
  }

  public void loadUserDictionary(Path path) throws IOException {
    try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
      this.userDictionary = UserDictionary.open(reader);
    }
  }

  public void resetUserDictionary() {
    this.userDictionary = null;
  }

  public CharArraySet getStopWords() {
    return stopWords;
  }

  public void setStopWords(CharArraySet stopWords) {
    this.stopWords = stopWords;
  }

  public void loadStopWordsDictionary(Path stopWordsDictionary, boolean withDefault) throws IOException {
    this.stopWords = new CharArraySet(Files.readAllLines(stopWordsDictionary, StandardCharsets.UTF_8), true);
    if (withDefault) {
      this.stopWords.addAll(JapaneseAnalyzer.getDefaultStopSet());
    }
  }

  public void resetStopWords() {
    this.stopWords = JapaneseAnalyzer.getDefaultStopSet();
  }

  public Set<String> getStopTags() {
    return stopTags;
  }

  public void setStopTags(Set<String> stopTags) {
    this.stopTags = stopTags;
  }

  public Mode getMode() {
    return mode;
  }

  public void setMode(Mode mode) {
    this.mode = mode;
  }

  public List<KuromojiResult> analyze(String text) {
    try (JapaneseAnalyzer japaneseAnalyzer = new JapaneseAnalyzer(userDictionary, mode, stopWords, stopTags)) {
      TokenStream tokenStream = japaneseAnalyzer.tokenStream("tokenStream", text);
      CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
      ReadingAttribute readingAttribute = tokenStream.addAttribute(ReadingAttribute.class);
      PartOfSpeechAttribute partOfSpeechAttribute = tokenStream.addAttribute(PartOfSpeechAttribute.class);

      tokenStream.reset();

      List<KuromojiResult> kuromojiResults = new ArrayList<>();
      while (tokenStream.incrementToken()) {
        KuromojiResult kuromojiResult = new KuromojiResult();
        kuromojiResult.setCharTerm(charTermAttribute.toString());
        kuromojiResult.setReading(readingAttribute.getReading());
        kuromojiResult.setPartOfSpeech(partOfSpeechAttribute.getPartOfSpeech());
        kuromojiResults.add(kuromojiResult);
      }
      return kuromojiResults;
    } catch (IOException e) {
      logger.error("failed analyze by kuromoji");
      throw new ApplicationException(e);
    }

  }

  public List<KuromojiResult> analyzeNouns(String text) {
    return analyze(text).stream().filter(kuromojiResult -> kuromojiResult.getPartOfSpeech().contains("名詞"))
        .collect(Collectors.toList());
  }

}
