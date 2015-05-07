HBase Observers examples
========================

Examples of HBase observers implementations.

Hbase version: 0.98.6-cdh5.3.1

TableHistory: Log Puts and Deletes on a Table into a History Table
------------------------------------------------------------------
For every Put on table A (with a row key "\<rowid\>"), a cell is put into a history table A_s with a row key "\<row id\>|\<cell's timestamp\>|P' and same column and value.
For every Delete on table A (with a row key "\<rowid\>"), only if the cell exists (i.e. it is actually deleted) a cell is put into a history table with a row key "\<row id\>|\<cell's timestamp\>|D' and same column but empty value.

### How to set the observer on a table named 'test_observer'

First load the jar somewhere into HDFS.
Start hbase-shell and create a table 'test_observer' and a table 'test_observer_s', which is the history table for 'test_observer'.

    hbase(main):049:0> alter 'test_observer', METHOD => 'table_att', 'coprocessor' => 'hdfs:///user/test/hbase-observer-0.1.jar|eu.unicredit.hbase.observer.TableHistory||'

### How to unset the observer on a table named 'test_observer'

    hbase(main):049:0> alter 'test_observer', METHOD => 'table_att_unset', NAME => 'coprocessor$1'
    
Replica: copy the row content from a Table into a column's cell of a "Master" Table
-----------------------------------------------------------------------------------
For every Put on table A (with a row key "\<rowid\>"), a cell is put into a master table with row key "\<row id\>", qualifier "\<row id\>" and value a concatenation of qualifiers and values of the row in A. Master table name and column family are passed as arguments.
For every Delete on table A (with a row key "\<rowid\>"), only if the row in A is not empty, it works as the Put case. Otherwise, the column on the master table is deleted.

### How to set the observer on a table named 'test_observer'

First load the jar somewhere into HDFS.
Start hbase-shell and create a table 'test_replica_child' and a table 'test_replica_master', which is the master table for 'test_replica_child'.

    hbase(main):049:0> alter 'test_replica_child', METHOD => 'table_att', 'coprocessor' => 'hdfs:///user/test/hbase-observer-0.1.jar|eu.unicredit.hbase.observer.Replica||master=test_replica_master,family=child'

### How to unset the observer on a table named 'test_observer'

    hbase(main):049:0> alter 'test_replica_child', METHOD => 'table_att_unset', NAME => 'coprocessor$1'
    
Note that if you make changes to the implementation and need to update the jar, you must change the jar name for HBase to load the new version. It is not enough to replace the jar into HDFS and unset/set the same coprocessor. Apparently HBase keeps a cache of the coprocessor's jar file.
