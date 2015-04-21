package eu.unicredit.hbase.observer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.coprocessor.BaseRegionObserver;
import org.apache.hadoop.hbase.coprocessor.ObserverContext;
import org.apache.hadoop.hbase.coprocessor.RegionCoprocessorEnvironment;
import org.apache.hadoop.hbase.regionserver.wal.WALEdit;
import org.apache.hadoop.hbase.util.Bytes;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.IOException;
import java.util.Arrays;

public class TableHistory extends BaseRegionObserver {
    public static final Log LOG = LogFactory.getLog(TableHistory.class);

    private static DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyyMMddHHmm");

    private static byte[] concatTwoByteArrays(byte[] first, byte[] second) {
        byte[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    private static byte[] createHistoryRow(byte[] row, long timestamp, char suffix) {
        DateTime timeStamp = new DateTime(timestamp);
        byte[] rowSuffix = Bytes.toBytes(String.format("|%s|%s", timeStamp.toString(fmt), suffix));
        return concatTwoByteArrays(row, rowSuffix);
    }

    @Override
    public void postPut(ObserverContext<RegionCoprocessorEnvironment> e,
                        Put put, WALEdit edit, Durability durability) throws IOException {
        char suffix = 'P';

        RegionCoprocessorEnvironment env = e.getEnvironment();
        String thisTableName = Bytes.toString(env.getRegion().getTableDesc().getName());

        HTableInterface historyTable = env.getTable(TableName.valueOf(String.format("%s_s", thisTableName)));

        byte[] row = put.getRow();

        CellScanner scanner = put.cellScanner();
        while (scanner.advance()) {
            Cell cell = scanner.current();

            long timestamp = cell.getTimestamp();
            byte[] columnFamily = CellUtil.cloneFamily(cell);
            byte[] qualifier = CellUtil.cloneQualifier(cell);
            byte[] value = CellUtil.cloneValue(cell);

            byte[] historyRow = createHistoryRow(row, timestamp, suffix);

            Put historyPut = new Put(historyRow);
            historyPut.add(columnFamily, qualifier, timestamp, value);

            LOG.info(String.format("%s %s %s:%s %s", suffix, Bytes.toString(row), Bytes.toString(columnFamily), Bytes.toString(qualifier), Bytes.toString(value)));

            historyTable.put(historyPut);
        }

        historyTable.close();
    }

    @Override
    public void preDelete(ObserverContext<RegionCoprocessorEnvironment> e,
                          Delete delete, WALEdit edit, Durability durability) throws IOException {
        char suffix = 'D';

        RegionCoprocessorEnvironment env = e.getEnvironment();
        String thisTableName = Bytes.toString(env.getRegion().getTableDesc().getName());

        HTableInterface historyTable = env.getTable(TableName.valueOf(String.format("%s_s", thisTableName)));
        HTableInterface thisTable = env.getTable(TableName.valueOf(thisTableName));

        byte[] row = delete.getRow();

        CellScanner scanner = delete.cellScanner();
        while (scanner.advance()) {
            Cell cell = scanner.current();

            byte[] columnFamily = CellUtil.cloneFamily(cell);
            byte[] qualifier = CellUtil.cloneQualifier(cell);

            Get get = new Get(row);
            get.addColumn(columnFamily, qualifier);
            if (thisTable.exists(get)) {
                long timestamp = (new DateTime()).getMillis();
                byte[] value = new byte[0];

                byte[] historyRow = createHistoryRow(row, timestamp, suffix);

                Put historyPut = new Put(historyRow);
                historyPut.add(columnFamily, qualifier, timestamp, value);

                LOG.info(String.format("%s %s %s:%s %s", suffix, Bytes.toString(row), Bytes.toString(columnFamily), Bytes.toString(qualifier), Bytes.toString(value)));

                historyTable.put(historyPut);
            }
        }

        historyTable.close();
    }
}