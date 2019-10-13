package com.org.devg.elasticsearch;

import java.util.List;
import lombok.Data;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregations;

@Data
public class EsResult<T> {

  private long total;
  private Aggregations aggregations;
  private List<T> esResultList;

  public static EsResult getEsResult(List esResultList, SearchResponse searchResponse) {
    SearchHits hits = searchResponse.getHits();
    EsResult esResult = new EsResult();
    esResult.setTotal(hits.getTotalHits().value);
    esResult.setAggregations(searchResponse.getAggregations());
    esResult.setEsResultList(esResultList);
    return esResult;
  }
}
