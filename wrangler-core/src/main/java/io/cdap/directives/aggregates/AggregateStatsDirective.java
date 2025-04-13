/*
* Copyright © 2017-2019 Cask Data, Inc.
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy of
* the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations under
* the License.
*/
package io.cdap.directives.aggregates;

import io.cdap.cdap.api.annotation.Description;
import io.cdap.cdap.api.annotation.Name;
import io.cdap.cdap.api.annotation.Plugin;
import io.cdap.wrangler.api.Arguments;
import io.cdap.wrangler.api.Directive;
import io.cdap.wrangler.api.DirectiveExecutionException;
import io.cdap.wrangler.api.DirectiveParseException;
import io.cdap.wrangler.api.ExecutorContext;
import io.cdap.wrangler.api.Optional;
import io.cdap.wrangler.api.Row;
import io.cdap.wrangler.api.TransientStore;
import io.cdap.wrangler.api.TransientVariableScope;
import io.cdap.wrangler.api.annotations.Categories;
import io.cdap.wrangler.api.parser.ByteSize;
import io.cdap.wrangler.api.parser.ColumnName;
import io.cdap.wrangler.api.parser.Text;
import io.cdap.wrangler.api.parser.TimeDuration;
import io.cdap.wrangler.api.parser.TokenType;
import io.cdap.wrangler.api.parser.UsageDefinition;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
/**
* Directive for aggregating byte sizes and time durations across multiple records.
* Example usage:
* aggregate-stats :memSize :processTime :totalSize :totalTime GB minutes total;
*/
@Plugin(type = Directive.TYPE)
@Name("aggregate-stats")
@Categories(categories = {"aggregator", "stats"})
@Description("Aggregates byte sizes and time durations from source columns into target columns")
public class AggregateStatsDirective implements Directive {
  public static final String NAME = "aggregate-stats";
  
  // Column names from arguments
  private String sourceSizeColumn;
  private String sourceDurationColumn;
  private String targetSizeColumn;
  private String targetDurationColumn;
  private String sizeUnit;
  private String timeUnit;
  private String aggregationType;
  // Add these constants near other static fields
  private static final String SIZE_VALUES_KEY = "aggregate.stats.size.values";
  private static final String TIME_VALUES_KEY = "aggregate.stats.time.values";  
  
  @Override
  public UsageDefinition define() {
    UsageDefinition.Builder builder = UsageDefinition.builder(NAME);
    
    // Required arguments
    builder.define("sourceSizeCol", TokenType.COLUMN_NAME);
    builder.define("sourceDurationCol", TokenType.COLUMN_NAME);
    builder.define("targetSizeCol", TokenType.COLUMN_NAME);
    builder.define("targetDurationCol", TokenType.COLUMN_NAME);
    
    // Optional arguments with defaults
    builder.define("sizeUnit", TokenType.TEXT, Optional.TRUE);
    builder.define("timeUnit", TokenType.TEXT,  Optional.TRUE);
    builder.define("aggregationType", TokenType.TEXT,  Optional.TRUE);
    
    return builder.build();
  }
  
  @Override
  public void initialize(Arguments args) throws DirectiveParseException {
    // Store required column names
    this.sourceSizeColumn = ((ColumnName) args.value("sourceSizeCol")).value();
    this.sourceDurationColumn = ((ColumnName) args.value("sourceDurationCol")).value();
    this.targetSizeColumn = ((ColumnName) args.value("targetSizeCol")).value();
    this.targetDurationColumn = ((ColumnName) args.value("targetDurationCol")).value();

    // Get optional arguments with validation
    Text sizeUnitArg = args.value("sizeUnit") != null ? 
    (Text) args.value("sizeUnit") : new Text("MB");
    Text timeUnitArg = args.value("timeUnit") != null ? 
    (Text) args.value("timeUnit") : new Text("s");
    Text aggTypeArg = args.value("aggregationType") != null ? 
    (Text) args.value("aggregationType") : new Text("total");
    
    this.sizeUnit = sizeUnitArg.value();
    this.timeUnit = timeUnitArg.value();
    this.aggregationType = aggTypeArg.value();
    
    // Validate units and aggregation type
    validateSizeUnit(this.sizeUnit);
    validateTimeUnit(this.timeUnit);
    validateAggregationType(this.aggregationType);
  }
  
  private void validateSizeUnit(String unit) throws DirectiveParseException {
    if (!unit.matches("^(B|KB|MB|GB|TB)$")) {
      throw new DirectiveParseException(
      String.format("Invalid size unit '%s'. Must be one of: B, KB, MB, GB, TB", unit));
    }
  }
  
  private void validateTimeUnit(String unit) throws DirectiveParseException {
    if (!unit.matches("^(ms|s|min|h|d)$")) {
      throw new DirectiveParseException(
      String.format("Invalid time unit '%s'. Must be one of: ms, s, min, h, d", unit));
    }
  }
  
  private void validateAggregationType(String type) throws DirectiveParseException {
    if (!type.matches("^(total|avg|mean|p95|p99)$")) {
      throw new DirectiveParseException(
      String.format("Invalid aggregation type '%s'. Must be one of: total, avg, mean, p95, p99", type));
    }
  }
  
  @Override
  public void destroy() {
    // Clean-up if needed
  }
  
  @Override
  public List<Row> execute(List<Row> rows, ExecutorContext context) throws DirectiveExecutionException {
      if (rows.isEmpty()) {
          return rows;
      }
  
      TransientStore store = context.getTransientStore();
      
      // Get or initialize value lists and row counter
      @SuppressWarnings("unchecked")
      List<Double> sizeValues = (List<Double>) store.get(SIZE_VALUES_KEY);
      @SuppressWarnings("unchecked")
      List<Double> timeValues = (List<Double>) store.get(TIME_VALUES_KEY);
      Integer processedRows = store.get("aggregate.stats.processed.rows");
      Integer totalRows = store.get("total_records");
      
      if (sizeValues == null) sizeValues = new ArrayList<>();
      if (timeValues == null) timeValues = new ArrayList<>();
      if (processedRows == null) processedRows = 0;
  
      // Process current row
      for (Row currentRow: rows){
        Object sizeValue = currentRow.getValue(sourceSizeColumn);
        Object timeValue = currentRow.getValue(sourceDurationColumn);
        
        if (sizeValue != null && timeValue != null) {
            try {
                ByteSize size = new ByteSize(sizeValue.toString());
                TimeDuration duration = new TimeDuration(timeValue.toString());
                
                sizeValues.add((double) size.getBytes());
                timeValues.add((double) duration.getNanoSeconds());
            } catch (IllegalArgumentException e) {
                throw new DirectiveExecutionException(
                    String.format("Invalid format in row: size='%s', time='%s'", 
                    sizeValue, timeValue));
            }
        }
        
        // Increment processed rows counter
        processedRows++;
      }
      // Update store
      store.set(TransientVariableScope.GLOBAL, SIZE_VALUES_KEY, sizeValues);
      store.set(TransientVariableScope.GLOBAL, TIME_VALUES_KEY, timeValues);
      store.set(TransientVariableScope.GLOBAL, "aggregate.stats.processed.rows", processedRows);
      
      // Check if this is the last row
      boolean isLastRow = processedRows.equals(totalRows);
      
      if (isLastRow) {
          // Calculate final values
          double finalSize;
          double finalDuration;
          int count = sizeValues.size();
          
          // Calculate based on aggregation type
          switch (aggregationType.toLowerCase()) {
              case "p95":
                  finalSize = calculatePercentile(sizeValues, 95);
                  finalDuration = calculatePercentile(timeValues, 95);
                  break;
              case "p99":
                  finalSize = calculatePercentile(sizeValues, 99);
                  finalDuration = calculatePercentile(timeValues, 99);
                  break;
              case "mean":
              case "avg":
                  finalSize = count > 0 ? sizeValues.stream().mapToDouble(Double::doubleValue).average().orElse(0.0) : 0.0;
                  finalDuration = count > 0 ? timeValues.stream().mapToDouble(Double::doubleValue).average().orElse(0.0) : 0.0;
                  break;
              case "total":
              default:
                  finalSize = sizeValues.stream().mapToDouble(Double::doubleValue).sum();
                  finalDuration = timeValues.stream().mapToDouble(Double::doubleValue).sum();
                  break;
          }
          
          // Convert to requested units
          finalSize = convertBytesToUnit(finalSize, sizeUnit);
          finalDuration = convertTimeToUnit(finalDuration, timeUnit);
          
          // Create result row
          Row result = new Row();
          result.add(targetSizeColumn, finalSize);
          result.add(targetDurationColumn, finalDuration);
          
          // Clean up store
          store.reset(TransientVariableScope.GLOBAL);
          
          return Collections.singletonList(result);
      }
      
      // For all rows except the last, return empty list
      return new ArrayList<>();
  }
  
  private double convertBytesToUnit(double bytes, String targetUnit) {
    switch (targetUnit) {
      case "B": return bytes;
      case "KB": return bytes / 1024.0;
      case "MB": return bytes / (1024.0 * 1024.0);
      case "GB": return bytes / (1024.0 * 1024.0 * 1024.0);
      case "TB": return bytes / (1024.0 * 1024.0 * 1024.0 * 1024.0);
      default: return bytes;
    }
  }
  
  private double convertTimeToUnit(double nanos, String targetUnit) {
    switch (targetUnit) {
      case "ns": return nanos;
      case "ms": return nanos / (1000*1000);
      case "s": return nanos / (1000*1000*1000);
      case "min": return nanos / (60.0 * 1000.0*1000*1000);
      case "h": return nanos / (60.0 * 60.0 * 1000.0*1000*1000);
      case "d": return nanos / (24.0 * 60.0 * 60.0 * 1000.0*1000*1000);
      default: return nanos;
    }
  }
  
  private double calculatePercentile(List<Double> values, int percentile) {
    if (values == null || values.isEmpty()) {
      return 0.0;
    }
    
    Collections.sort(values);
    int index = (int) Math.ceil(percentile / 100.0 * values.size()) - 1;
    return values.get(Math.max(0, index));
  }
}
