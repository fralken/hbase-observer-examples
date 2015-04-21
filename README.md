HBase Observers examples
========================

Examples of HBase observers implementations.

Hbase version: 0.98.6-cdh5.3.1

TableHistory: Log Puts and Deletes on a Table into a History Table
----------------------------------------------------
For every Put on table A (with a row key "\<rowid\>"), a cell is put into a history table with a row key "\<row id\>|\<cell's timestamp\>|P' and same column and value.
For every Delete on table A (with a row key "\<rowid\>"), only if the cell exists (i.e. it is actually deleted) a cell is put into a history table with a row key "\<row id\>|\<cell's timestamp\>|D' and same column but empty value.

### How to set the observer on a table named 'test_observer'

First load the jar somewhere into HDFS.
Start hbase-shell and create a table 'test_observer' and a table 'test_observer_s', which is the history table for 'test_observer'.

    hbase(main):049:0> alter 'test_observer', METHOD => 'table_att', 'coprocessor' => 'hdfs:///user/test/hbase-observer-0.1.jar|eu.unicredit.hbase.observer.TableHistory||'

### How to unset the observer on a table named 'test_observer'

    hbase(main):049:0> alter 'test_observer', METHOD => 'table_att_unset', NAME => 'coprocessor$1'
    
Note that if you make changes to the implementation and need to update the jar, you must change the jar name for HBase to load the new version. It is not enough to replace the jar into HDFS and unset/set the same coprocessor. Apparently HBase keeps a cache of the coprocessor's jar file.

