package com.course.aggregator.storage;

import com.course.aggregator.exception.StorageException;
import com.course.aggregator.model.AggregatedItem;

import java.nio.file.Path;
import java.util.List;

public interface DataStorage {

  void write(Path path, List<AggregatedItem> items, WriteMode mode) throws StorageException;

  List<AggregatedItem> readAll(Path path) throws StorageException;

  List<AggregatedItem> readBySource(Path path, String sourceName) throws StorageException;

  String format();
}
