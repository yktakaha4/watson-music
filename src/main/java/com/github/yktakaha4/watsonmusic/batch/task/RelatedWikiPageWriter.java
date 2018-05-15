package com.github.yktakaha4.watsonmusic.batch.task;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.github.yktakaha4.watsonmusic.model.Text;
import com.github.yktakaha4.watsonmusic.model.TextTag;
import com.github.yktakaha4.watsonmusic.model.WikiPage;
import com.github.yktakaha4.watsonmusic.service.TextService;
import com.github.yktakaha4.watsonmusic.service.TextTagService;

@Controller
@StepScope
public class RelatedWikiPageWriter implements ItemWriter<Pair<Text, List<WikiPage>>> {
  private final TextService textService;
  private final TextTagService textTagService;

  @Autowired
  public RelatedWikiPageWriter(TextService textService, TextTagService textTagService) {
    this.textService = textService;
    this.textTagService = textTagService;
  }

  @Override
  public void write(List<? extends Pair<Text, List<WikiPage>>> pairs) throws Exception {
    pairs.forEach(this::write);
  }

  private void write(Pair<Text, List<WikiPage>> pair) {
    Text text = pair.getLeft();
    List<Text> relatedTexts = pair.getRight().stream().map((wikiPage) -> {
      Text relatedText = textService.createText(wikiPage);
      textService.manage(relatedText);
      return textService.getText(relatedText.getSource(), relatedText.getTitle()).get();
    }).collect(Collectors.toList());

    List<TextTag> textTags = textTagService.getRelationsByText(text);
    int textRelevance = 0;
    for (Text relatedText : relatedTexts) {
      List<String> relatedTextTags = textTagService.getRelationsByText(relatedText).stream()
          .map(textTag -> textTag.getTag()).collect(Collectors.toList());
      for (TextTag textTag : textTags.stream().filter((textTag) -> {
        return !relatedTextTags.contains(textTag.getTag());
      }).collect(Collectors.toList())) {
        int relevance = textTag.getRelevance() + textRelevance;
        textTagService.addRelation(relatedText, textTag.getTag(), relevance);
      }
      textRelevance++;
    }
  }
}
