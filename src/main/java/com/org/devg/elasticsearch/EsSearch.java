package com.org.devg.elasticsearch;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.lucene.search.TotalHits;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class EsSearch {

  private static final Logger logger = LoggerFactory.getLogger(EsSearch.class);

  @Autowired
  private RestHighLevelClient restHighLevelClient;

  public <T> T getEsResultEntity(Map<String, Object> map, Class<T> sourceType) throws Exception {
    T t = sourceType.newInstance();
    Field[] fields = sourceType.getDeclaredFields();
    for (Field field : fields) {
      EsCloumn esCloumn = field.getAnnotation(EsCloumn.class);
      if (esCloumn != null) {
        field.setAccessible(true);
        String esName = esCloumn.name();
        boolean esArray = esCloumn.isArray();
        Object esValue = null;
        if (esArray) {
          String arrayValue = JSON.toJSONString(map.get(esName));
          Class<T> arrClazz = esCloumn.clazz();
          JSONArray jsonArray = new JSONArray();
          if (null != arrayValue && !arrayValue.equals("")) {
            jsonArray = JSON.parseArray(arrayValue);
            if (null == jsonArray || jsonArray.size() <= 0) {
              continue;
            }
            int arraySize = jsonArray.size();
            T[] arrDatas = (T[]) Array.newInstance(arrClazz, arraySize); //创建一个数组
            for (int i = 0; i < arraySize; i++) {
              T arrData = arrClazz.newInstance();
              Field[] esFields = arrClazz.getDeclaredFields();
              JSONObject jsonObject = jsonArray.getJSONObject(i);
              for (Field arrfield : esFields) {
                arrfield.setAccessible(true);
                Object arrEsValue = null;
                arrEsValue = getEsValue(arrfield, jsonObject);
                Field arrEntityField = arrClazz.getDeclaredField(arrfield.getName());
                arrEntityField.setAccessible(true);
                arrEntityField.set(arrData, arrEsValue);
              }
              arrDatas[i] = arrData;
            }
            esValue = arrDatas;
          }
        } else {
          esValue = getEsValue(field, map);
        }
        Field entityField = t.getClass().getDeclaredField(field.getName());
        entityField.setAccessible(true);
        entityField.set(t, esValue);
        continue;
      }
    }
    return t;
  }

  public Object getEsValue(Field field, Map<String, Object> map) {
    Object esValue = null;
    EsCloumn esCloumn = field.getAnnotation(EsCloumn.class);
    if (esCloumn != null) {
      String dateFormat = esCloumn.dateformat();
      String esName = esCloumn.name();
      String esTypeName = field.getType().getName();
      switch (esTypeName) {
        case "java.lang.Integer":
          esValue = map.get(esName) == null ? null : Integer.valueOf(map.get(esName).toString());
          break;
        case "java.lang.Long":
          esValue = map.get(esName) == null ? null : Long.valueOf(map.get(esName).toString());
          break;
        case "java.lang.String":
          esValue = map.get(esName) == null ? null : String.valueOf(map.get(esName));
          if (null != dateFormat && !dateFormat.equals("")) {
            esValue = esValue == null ? null : EsUitl.transferLongToDate(dateFormat, Long.valueOf(esValue.toString()));
          }
          break;
        case "java.lang.Double":
          esValue = map.get(esName) == null ? null : Double.valueOf(map.get(esName).toString());
          break;
        case "java.math.BigDecimal":
          esValue = map.get(esName) == null ? null : new BigDecimal(map.get(esName).toString());
          break;
        default:
          esValue = map.get(esName) == null ? null : String.valueOf(map.get(esName));
          if (null != dateFormat && !dateFormat.equals("")) {
            esValue = esValue == null ? null : EsUitl.transferLongToDate(dateFormat, Long.valueOf(esValue.toString()));
          }
          break;
      }
    }
    return esValue;
  }

  public <T> List<T> getEsResultList(SearchHit[] searchHits, Class<T> sourceType) throws Exception {
    List<T> resultList = new ArrayList<>();
    if (null == searchHits || searchHits.length <= 0) {
      return resultList;
    }
    for (SearchHit hit : searchHits) {
      Map<String, Object> sourceAsMap = hit.getSourceAsMap();
      T result = this.getEsResultEntity(sourceAsMap, sourceType);
      resultList.add(result);
    }
    return resultList;
  }

  public <T> List<T> getEsResultList(SearchResponse searchResponse, Class<T> sourceType) throws Exception {
    SearchHits hits = searchResponse.getHits();
    SearchHit[] searchHits = hits.getHits();
    return getEsResultList(searchHits, sourceType);
  }

  public <T> List<T> getEsResultList(SearchSourceBuilder searchSourceBuilder, Class<T> sourceType, String... indices) throws Exception {
    SearchHits hits = this.getSearchHits(searchSourceBuilder, indices);
    SearchHit[] searchHits = hits.getHits();
    return getEsResultList(searchHits, sourceType);
  }


  public List<Map<String, Object>> getEsResultList(SearchSourceBuilder searchSourceBuilder, String... indices) throws Exception {
    List<Map<String, Object>> resultList = new ArrayList<>();
    SearchHits hits = this.getSearchHits(searchSourceBuilder, indices);
    SearchHit[] searchHits = hits.getHits();
    for (SearchHit hit : searchHits) {
      Map<String, Object> sourceAsMap = hit.getSourceAsMap();
      resultList.add(sourceAsMap);
    }
    return resultList;
  }

  public SearchRequest getSearchRequest(SearchSourceBuilder searchSourceBuilder, String... indices) {
    logger.info("elasticsearch-query:" + searchSourceBuilder.toString());
    SearchRequest searchRequest = new SearchRequest();
    searchRequest.source(searchSourceBuilder);
    searchRequest.indices(indices);
    return searchRequest;
  }

  public SearchResponse getSearchResponse(SearchSourceBuilder searchSourceBuilder, String... indices) throws IOException {
    searchSourceBuilder.trackTotalHits(true);
    SearchRequest searchRequest = this.getSearchRequest(searchSourceBuilder, indices);
    SearchResponse searchResponse = null;
    try {
      searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
    } catch (Exception ex) {
      logger.error("search-exception:", ex);
      try {
        searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
      } catch (Exception e) {
        logger.error("search-exception-1:", ex);
      }
    }
    return searchResponse;
  }

  public SearchHits getSearchHits(SearchSourceBuilder searchSourceBuilder, String... indices) throws IOException {
    return this.getSearchResponse(searchSourceBuilder, indices).getHits();
  }

  public TotalHits getTotalHits(SearchSourceBuilder searchSourceBuilder, String... indices) throws IOException {
    return this.getSearchHits(searchSourceBuilder, indices).getTotalHits();
  }

  public long getTotalValue(SearchSourceBuilder searchSourceBuilder, String... indices) throws IOException {
    return this.getTotalHits(searchSourceBuilder, indices).value;
  }

  public Aggregations getAggregations(SearchSourceBuilder searchSourceBuilder, String... indices) throws IOException {
    return this.getSearchResponse(searchSourceBuilder, indices).getAggregations();
  }

  public <T> EsResult<T> getEsResult(SearchSourceBuilder searchSourceBuilder, Class<T> sourceType, Integer pageNum, Integer pageSize,
      String... indices) throws Exception {
    searchSourceBuilder.from((pageNum - 1) * pageSize);
    searchSourceBuilder.size(pageSize);
    return getEsResult(searchSourceBuilder, sourceType, indices);
  }

  public <T> EsResult<T> getEsResult(SearchSourceBuilder searchSourceBuilder, Class<T> sourceType, String... indices) throws Exception {
    SearchResponse searchResponse = this.getSearchResponse(searchSourceBuilder, indices);
    SearchHits hits = searchResponse.getHits();
    List<T> esResultList = this.getEsResultList(hits.getHits(), sourceType);
    return EsResult.getEsResult(esResultList, searchResponse);
  }

  public EsResult<Map<String, Object>> getEsResult(SearchSourceBuilder searchSourceBuilder, Integer pageNum, Integer pageSize,
      String... indices) throws Exception {
    searchSourceBuilder.from((pageNum - 1) * pageSize);
    searchSourceBuilder.size(pageSize);
    return getEsResult(searchSourceBuilder, indices);
  }

  public EsResult<Map<String, Object>> getEsResult(SearchSourceBuilder searchSourceBuilder, String... indices) throws Exception {
    SearchResponse searchResponse = this.getSearchResponse(searchSourceBuilder, indices);
    SearchHits hits = searchResponse.getHits();
    List<Map<String, Object>> resultList = new ArrayList<>();
    for (SearchHit hit : hits.getHits()) {
      Map<String, Object> sourceAsMap = hit.getSourceAsMap();
      resultList.add(sourceAsMap);
    }
    return EsResult.getEsResult(resultList, searchResponse);
  }

}
