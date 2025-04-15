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
import io.cdap.wrangler.api.EntityCountMetric;
import io.cdap.wrangler.api.ExecutorContext;
import io.cdap.wrangler.api.Row;
import io.cdap.wrangler.api.TransientStore;
import io.cdap.wrangler.api.TransientVariableScope;
import io.cdap.wrangler.api.parser.ByteSize;
import io.cdap.wrangler.api.parser.ColumnName;
import io.cdap.wrangler.api.parser.Text;
import io.cdap.wrangler.api.parser.TimeDuration;
import io.cdap.wrangler.api.parser.TokenType;
import io.cdap.wrangler.api.parser.UsageDefinition;

import java.util.Collections;
import java.util.List;

/**
 * A directive to aggregate byte sizes and time durations into totals or
 * averages.
 */
@Plugin(type = Directive.TYPE)
@Name("aggregate-stats")
@Description("Aggregates byte sizes and time durations into total or average values.")
public class AggregateStats implements Directive {
    private String sizeCol;
    private String timeCol;
    private String totalSizeCol;
    private String totalTimeCol;
    private String sizeUnit;
    private String timeUnit;
    private String aggType;
    private boolean isLastCall = false;

    private static final String TOTAL_BYTES_KEY = "aggregate-stats.totalBytes";
    private static final String TOTAL_NANOS_KEY = "aggregate-stats.totalNanos";
    private static final String ROW_COUNT_KEY = "aggregate-stats.rowCount";

    @Override
    public UsageDefinition define() {
        UsageDefinition.Builder builder = UsageDefinition.builder("aggregate-stats");
        builder.define("sizeCol", TokenType.COLUMN_NAME); // Source column with byte sizes
        builder.define("timeCol", TokenType.COLUMN_NAME); // Source column with time durations
        builder.define("totalSize", TokenType.COLUMN_NAME); // Target column for total size
        builder.define("totalTime", TokenType.COLUMN_NAME); // Target column for total/average time
        builder.define("sizeUnit", TokenType.TEXT, true); // Optional: MB, GB (default: bytes)
        builder.define("timeUnit", TokenType.TEXT, true); // Optional: seconds, minutes (default: nanoseconds)
        builder.define("aggType", TokenType.TEXT, true); // Optional: total, average (default: total)
        return builder.build();
    }

    @Override
    public void initialize(Arguments args) {
        sizeCol = ((ColumnName) args.value("sizeCol")).value();
        timeCol = ((ColumnName) args.value("timeCol")).value();
        totalSizeCol = ((ColumnName) args.value("totalSize")).value();
        totalTimeCol = ((ColumnName) args.value("totalTime")).value();
        sizeUnit = args.value("sizeUnit") != null ? ((Text) args.value("sizeUnit")).value() : "bytes";
        timeUnit = args.value("timeUnit") != null ? ((Text) args.value("timeUnit")).value() : "nanoseconds";
        aggType = args.value("aggType") != null ? ((Text) args.value("aggType")).value() : "total";
    }

    @Override
    public List<Row> execute(List<Row> rows, ExecutorContext context) throws DirectiveExecutionException {
        TransientStore store = context.getTransientStore();
        long totalBytes = store.get(TOTAL_BYTES_KEY) != null ? (Long) store.get(TOTAL_BYTES_KEY) : 0L;
        long totalNanos = store.get(TOTAL_NANOS_KEY) != null ? (Long) store.get(TOTAL_NANOS_KEY) : 0L;
        long rowCount = store.get(ROW_COUNT_KEY) != null ? (Long) store.get(ROW_COUNT_KEY) : 0L;

        // Detect last call (e.g., empty rows might indicate end)
        if (rows.isEmpty()) {
            isLastCall = true;
        }

        if (!isLastCall) {
            for (Row row : rows) {
                Object sizeObj = row.getValue(sizeCol);
                if (sizeObj != null) {
                    long bytes;
                    if (sizeObj instanceof String) {
                        bytes = new ByteSize((String) sizeObj).getBytes();
                    } else if (sizeObj instanceof ByteSize) {
                        bytes = ((ByteSize) sizeObj).getBytes();
                    } else {
                        continue; // Skip invalid values
                    }
                    totalBytes += bytes;
                }

                Object timeObj = row.getValue(timeCol);
                if (timeObj != null) {
                    long nanos;
                    if (timeObj instanceof String) {
                        nanos = new TimeDuration((String) timeObj).getNanos();
                    } else if (timeObj instanceof TimeDuration) {
                        nanos = ((TimeDuration) timeObj).getNanos();
                    } else {
                        continue; // Skip invalid values
                    }
                    totalNanos += nanos;
                }

                rowCount++;
            }

            store.set(TransientVariableScope.GLOBAL, TOTAL_BYTES_KEY, totalBytes);
            store.set(TransientVariableScope.GLOBAL, TOTAL_NANOS_KEY, totalNanos);
            store.set(TransientVariableScope.GLOBAL, ROW_COUNT_KEY, rowCount);
            return Collections.emptyList();
        } else {
            // Finalization: Compute and return aggregated results
            Long totalBytesFinal = (Long) store.get(TOTAL_BYTES_KEY);
            Long totalNanosFinal = (Long) store.get(TOTAL_NANOS_KEY);
            Long rowCountFinal = (Long) store.get(ROW_COUNT_KEY);
            totalBytesFinal = totalBytesFinal != null ? totalBytesFinal : 0L;
            totalNanosFinal = totalNanosFinal != null ? totalNanosFinal : 0L;
            rowCountFinal = rowCountFinal != null ? rowCountFinal : 0L;

            double sizeValue = totalBytesFinal;
            if ("MB".equalsIgnoreCase(sizeUnit)) {
                sizeValue = totalBytesFinal / (1024.0 * 1024.0);
            } else if ("GB".equalsIgnoreCase(sizeUnit)) {
                sizeValue = totalBytesFinal / (1024.0 * 1024.0 * 1024.0);
            }

            double timeValue = totalNanosFinal;
            if ("seconds".equalsIgnoreCase(timeUnit)) {
                timeValue = totalNanosFinal / 1_000_000_000.0;
            } else if ("minutes".equalsIgnoreCase(timeUnit)) {
                timeValue = totalNanosFinal / (60.0 * 1_000_000_000.0);
            }

            if ("average".equalsIgnoreCase(aggType) && rowCountFinal > 0) {
                sizeValue /= rowCountFinal;
                timeValue /= rowCountFinal;
            }

            Row result = new Row();
            result.add(totalSizeCol, sizeValue);
            result.add(totalTimeCol, timeValue);
            store.reset(TransientVariableScope.GLOBAL); // Clean up
            return Collections.singletonList(result);
        }
    }

    @Override
    public List<EntityCountMetric> getCountMetrics() {
        return Collections.emptyList(); // No metrics for now
    }

    @Override
    public void destroy() {
        // Cleanup if needed
    }
}
