/*
 *  Copyright © 2017-2019 Cask Data, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License. You may obtain a copy of
 *  the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */
package io.cdap.directives.aggregates;

import io.cdap.wrangler.TestingRig;
import io.cdap.wrangler.api.Row;
import io.cdap.wrangler.api.DirectiveParseException;
import io.cdap.wrangler.api.DirectiveExecutionException;
import org.junit.Assert;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;



public class AggregateStatsDirectiveTest {

    @Test
    public void testTotalAggregation() throws Exception {
        String[] recipe = new String[]{
            "aggregate-stats :size :time :total_size :total_time MB s total"
        };

        List<Row> rows = new ArrayList<>();
        // Add test data - sizes in bytes and times in nanoseconds
        rows.add(new Row("size", "1MB").add("time", "1s")); 
        rows.add(new Row("size", "2MB").add("time", "2s"));
        rows.add(new Row("size", "3MB").add("time", "3s"));

        List<Row> results = TestingRig.execute(recipe, rows);
        
        Assert.assertEquals(1, results.size());
        // Expected: 6MB total size, 6s total time
        Assert.assertEquals(6.0, (Double)results.get(0).getValue("total_size"), 0.001);
        Assert.assertEquals(6.0, (Double)results.get(0).getValue("total_time"), 0.001);
    }

    @Test
    public void testAverageAggregation() throws Exception {
        String[] recipe = new String[]{
            "aggregate-stats :size :time :avg_size :avg_time 'GB' 'min' 'avg'"
        };

        List<Row> rows = new ArrayList<>();
        rows.add(new Row("size", "1GB").add("time", "60min"));
        rows.add(new Row("size", "2GB").add("time", "120min"));
        rows.add(new Row("size", "3GB").add("time", "180min"));

        List<Row> results = TestingRig.execute(recipe, rows);
        
        Assert.assertEquals(1, results.size());
        // Expected: 2GB average size, 120min average time
        Assert.assertEquals(2.0, (Double)results.get(0).getValue("avg_size"), 0.001);
        Assert.assertEquals(120.0, (Double)results.get(0).getValue("avg_time"), 0.001);
    }

    @Test
    public void testPercentileAggregation() throws Exception {
        String[] recipe = new String[]{
            "aggregate-stats :size :time :p95_size :p95_time 'KB' 'ms' 'p95'"
        };

        List<Row> rows = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            rows.add(new Row("size", i + "KB").add("time", i + "ms"));
        }

        List<Row> results = TestingRig.execute(recipe, rows);
        
        Assert.assertEquals(1, results.size());
        // Expected: 95th percentile values
        Assert.assertEquals(95.0, (Double)results.get(0).getValue("p95_size"), 0.001);
        Assert.assertEquals(95.0, (Double)results.get(0).getValue("p95_time"), 0.001);
    }
   
    @Test
    public void testEmptyInput() throws Exception {
        String[] recipe = new String[]{
            "aggregate-stats :size :time :total_size :total_time 'MB' 's' 'total'"
        };
        
        List<Row> rows = new ArrayList<>();
        List<Row> results = TestingRig.execute(recipe, rows);
        
        Assert.assertEquals(rows, results);
    }
}