## ByteSize and TimeDuration Support

Starting with version 4.5.0, Data Prep supports byte size and time duration values in directives. These can be used to specify memory sizes and time intervals in a human-readable format.

### ByteSize Parser

The ByteSize parser supports the following formats:
- Basic units: B, KB, MB, GB, TB (case sensitive)
- Numbers can include decimals and scientific notation
- Examples: "1024B", "10KB", "1.5MB", "2.1GB", "1TB", "1.2e3KB"



### TimeDuration Parser 

The TimeDuration parser supports the following formats:
- Basic units: ms (milliseconds), s (seconds), min (minutes), h (hours), d (days)
- Numbers can include decimals and scientific notation  
- Examples: "150ms", "30s", "5min", "2.5h", "1d", "1.2e3ms"

### Usage with AggregateStats Directive

The new aggregate-stats directive demonstrates usage of both parsers:

```wrangler
// Basic usage - defaults to MB and seconds
aggregate-stats :memoryCol :timeCol :totalMem :totalTime;

// With specified units and aggregation type
aggregate-stats :memSize :procTime :totalSize :totalTime GB minutes total;

// With percentile aggregation
aggregate-stats :memCol :timeCol :p95mem :p95time GB minutes p95;
```

Parameters:
- Source column with byte sizes (:memoryCol)
- Source column with time durations (:timeCol) 
- Target column for aggregated size (:totalMem)
- Target column for aggregated time (:totalTime)
- Optional size unit (B|KB|MB|GB|TB) - defaults to MB
- Optional time unit (ms|s|min|h|d) - defaults to seconds
- Optional aggregation type (total|avg|p95|p99) - defaults to total

The directive will:
1. Parse byte sizes and time durations from source columns
2. Aggregate values based on specified type
3. Convert to requested output units
4. Store results in target columns

Example:
```wrangler
// Input rows
// :memSize    :procTime
// "2.5GB"     "30s"
// "1.5GB"     "45s"
// "3GB"       "25s"

aggregate-stats :memSize :procTime :totalSize :totalTime GB minutes total;

// Output row
// :totalSize  :totalTime
// 7.0         1.67
```