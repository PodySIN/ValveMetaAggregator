package com.course.aggregator.source;

import com.course.aggregator.exception.ApiRequestException;
import com.course.aggregator.exception.DataMappingException;
import com.course.aggregator.model.AggregatedItem;

import java.util.List;
import java.util.Map;

public interface DataSource {

  String getName();

  String getDescription();

  List<AggregatedItem> fetch(Map<String, String> queryParams)
      throws ApiRequestException, DataMappingException;
}
