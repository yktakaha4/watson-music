package com.github.yktakaha4.watsonmusic.util.kuromoji;

import com.orangesignal.csv.annotation.CsvColumn;
import com.orangesignal.csv.annotation.CsvEntity;

@CsvEntity(header = false)
public class Userdic {
  @CsvColumn(position = 0)
  public String term;

  @CsvColumn(position = 1)
  public String tokenizedTerms;

  @CsvColumn(position = 2)
  public String reading;

  @CsvColumn(position = 3)
  public String partOfSpeech;

}
