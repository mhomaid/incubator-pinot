/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.linkedin.pinot.core.segment.virtualcolumn;

import org.apache.pinot.common.data.FieldSpec;
import org.apache.pinot.common.utils.LLCSegmentName;
import org.apache.pinot.common.utils.Pairs;
import org.apache.pinot.core.io.reader.BaseSingleColumnSingleValueReader;
import org.apache.pinot.core.io.reader.DataFileReader;
import org.apache.pinot.core.io.reader.impl.ChunkReaderContext;
import org.apache.pinot.core.io.reader.impl.v1.SortedIndexReader;
import org.apache.pinot.core.io.reader.impl.v1.SortedIndexReaderImpl;
import org.apache.pinot.core.io.util.DictionaryDelegatingValueReader;
import org.apache.pinot.core.io.util.ValueReader;
import org.apache.pinot.core.segment.index.ColumnMetadata;
import org.apache.pinot.core.segment.index.readers.Dictionary;
import org.apache.pinot.core.segment.index.readers.IntDictionary;
import org.apache.pinot.core.segment.index.readers.InvertedIndexReader;
import org.apache.pinot.core.segment.virtualcolumn.BaseVirtualColumnProvider;
import org.apache.pinot.core.segment.virtualcolumn.VirtualColumnContext;

import java.io.IOException;

/**
 * this virtual column provide the partition id value to pinot data ingested from kafka as the partition id of the
 * source data
 */
public class PartitionVirtualColumnProvider extends BaseVirtualColumnProvider {

  @Override
  public DataFileReader buildReader(VirtualColumnContext context) {
    return new PartitionIdSingleValueReader(getPartitionIdFromContext(context));
  }

  @Override
  public Dictionary buildDictionary(VirtualColumnContext context) {
    DictionaryDelegatingValueReader valueReader = new DictionaryDelegatingValueReader();
    PartitionDictionary partitionDictionary = new PartitionDictionary(valueReader, context.getTotalDocCount(),
        getPartitionIdFromContext(context));
    valueReader.setDictionary(partitionDictionary);
    return partitionDictionary;
  }

  @Override
  public ColumnMetadata buildMetadata(VirtualColumnContext context) {
    ColumnMetadata.Builder columnMetadataBuilder = super.getColumnMetadataBuilder(context);
    columnMetadataBuilder.setCardinality(context.getTotalDocCount())
        .setHasDictionary(true)
        .setHasInvertedIndex(true)
        .setFieldType(FieldSpec.FieldType.DIMENSION)
        .setDataType(FieldSpec.DataType.INT)
        .setSingleValue(true)
        .setIsSorted(true);

    return columnMetadataBuilder.build();
  }

  @Override
  public InvertedIndexReader buildInvertedIndex(VirtualColumnContext context) {
    return new PartitionInvertedIndexReader(context.getTotalDocCount());
  }

  private static int getPartitionIdFromContext(VirtualColumnContext context) {
    LLCSegmentName segmentName = new LLCSegmentName(context.getSegmentName());
    return segmentName.getPartitionId();
  }

  private class PartitionIdSingleValueReader extends BaseSingleColumnSingleValueReader<ChunkReaderContext> {

    private final int _partitionId;

    public PartitionIdSingleValueReader(int partitionId) {
      _partitionId = partitionId;
    }

    @Override
    public ChunkReaderContext createContext() {
      return null;
    }

    @Override
    public int getInt(int row) {
      return _partitionId;
    }

    @Override
    public int getInt(int rowId, ChunkReaderContext context) {
      return _partitionId;
    }

    @Override
    public long getLong(int row) {
      return _partitionId;
    }

    @Override
    public long getLong(int rowId, ChunkReaderContext context) {
      return _partitionId;
    }

    @Override
    public void readValues(int[] rows, int rowStartPos, int rowSize, int[] values, int valuesStartPos) {
      System.arraycopy(rows, rowStartPos, values, valuesStartPos, rowSize);
    }

    @Override
    public void close() throws IOException {
    }
  }

  private class PartitionInvertedIndexReader extends BaseSingleColumnSingleValueReader<SortedIndexReaderImpl.Context> implements SortedIndexReader<SortedIndexReaderImpl.Context> {

    private final int _length;

    public PartitionInvertedIndexReader(int length) {
      _length = length;
    }

    @Override
    public Pairs.IntPair getDocIds(int dictId) {
      if (dictId == 0) {
        return new Pairs.IntPair(0, _length);
      }
      return new Pairs.IntPair(-1, -1);
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public SortedIndexReaderImpl.Context createContext() {
      return null;
    }

    @Override
    public int getInt(int row) {
      return row;
    }

    @Override
    public int getInt(int rowId, SortedIndexReaderImpl.Context context) {
      return rowId;
    }
  }

  private class PartitionDictionary extends IntDictionary {
    private final int _length;
    private final int _partition;

    public PartitionDictionary(ValueReader valueReader, int length, int partition) {
      super(valueReader, length);
      _length = length;
      _partition = partition;
    }

    @Override
    public int indexOf(Object rawValue) {
      if (rawValue != null && rawValue.equals(_partition)) {
        return 1;
      } else {
        return -1;
      }
    }

    @Override
    public Integer get(int dictId) {
      return _partition;
    }

    @Override
    public int getIntValue(int dictId) {
      return _partition;
    }

    @Override
    public long getLongValue(int dictId) {
      return _partition;
    }

    @Override
    public float getFloatValue(int dictId) {
      return _partition;
    }

    @Override
    public double getDoubleValue(int dictId) {
      return _partition;
    }

    @Override
    public String getStringValue(int dictId) {
      return Integer.toString(_partition);
    }

    @Override
    public int length() {
      return _length;
    }

    @Override
    public boolean isSorted() {
      return true;
    }

    @Override
    public void close() throws IOException {
    }
  }
}
