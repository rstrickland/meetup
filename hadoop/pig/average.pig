rows = LOAD 'cassandra://HadoopTest/TestInput' USING org.apache.cassandra.hadoop.pig.CassandraStorage() as (key:bytearray,cols:bag{col:tuple(name:chararray,value)});
columns = FOREACH rows GENERATE flatten(cols) as (name,value);
grouped = GROUP columns BY name;
vals = FOREACH grouped GENERATE group, columns.value AS values;
avgs = FOREACH vals GENERATE group, 'Pig_Average' as name, (long)SUM(values.value)/COUNT(values.value) AS average;    
cass_group = GROUP avgs BY group;   
cass_out = FOREACH cass_group GENERATE group, avgs.(name, average);
STORE cass_out INTO 'cassandra://HadoopTest/TestOutput' USING org.apache.cassandra.hadoop.pig.CassandraStorage();
