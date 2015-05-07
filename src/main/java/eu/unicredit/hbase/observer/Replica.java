package eu.unicredit.hbase.observer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.coprocessor.BaseRegionObserver;
import org.apache.hadoop.hbase.coprocessor.ObserverContext;
import org.apache.hadoop.hbase.coprocessor.RegionCoprocessorEnvironment;
import org.apache.hadoop.hbase.regionserver.wal.WALEdit;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;

/**
 * Created by US00472 on 05/05/15.
 */
public class Replica extends BaseRegionObserver {

    private byte[] master;
    private byte[] family;

    @Override
    public void start(CoprocessorEnvironment e) throws IOException {
        Configuration conf = e.getConfiguration();
        master = Bytes.toBytes(conf.get("master"));
        family = Bytes.toBytes(conf.get("family"));
    }

    private void insert(RegionCoprocessorEnvironment env, byte[] row, Result r) throws IOException {
        Table masterTable = env.getTable(TableName.valueOf(master));
        try {
            CellScanner scanner = r.cellScanner();
            StringBuilder str = new StringBuilder();
            int count = 0;
            while (scanner.advance()) {
                Cell cell = scanner.current();
                byte[] qualifier = CellUtil.cloneQualifier(cell);
                byte[] value = CellUtil.cloneValue(cell);
                if (count > 0)
                    str.append("|");
                str.append(Bytes.toString(qualifier))
                        .append("=")
                        .append(Bytes.toString(value));
                count++;
            }
            Put put = new Put(row);
            put.addColumn(family, row, Bytes.toBytes(str.toString()));
            masterTable.put(put);
        } finally {
            masterTable.close();
        }
    }

    private void remove(RegionCoprocessorEnvironment env, byte[] row) throws IOException {
        Table masterTable = env.getTable(TableName.valueOf(master));
        try {
            Delete delete = new Delete(row);
            delete.addColumns(family, row);
            masterTable.delete(delete);
        } finally {
            masterTable.close();
        }
    }

    @Override
    public void postPut(ObserverContext<RegionCoprocessorEnvironment> e, Put put, WALEdit edit, Durability durability) throws IOException {
        RegionCoprocessorEnvironment env = e.getEnvironment();

        byte[] row = put.getRow();
        Get get = new Get(row);
        Result result = env.getRegion().get(get);
        insert(env, row, result);
    }

    @Override
    public void postDelete(ObserverContext<RegionCoprocessorEnvironment> e, Delete delete, WALEdit edit, Durability durability) throws IOException {
        RegionCoprocessorEnvironment env = e.getEnvironment();

        byte[] row = delete.getRow();
        Get get = new Get(row);
        Result result = env.getRegion().get(get);
        if (result.isEmpty())
            remove(env, row);
        else
            insert(env, row, result);
    }
}
