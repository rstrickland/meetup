import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import org.apache.cassandra.db.IColumn;
import org.apache.cassandra.thrift.*;
import org.apache.cassandra.hadoop.*;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.util.*;
import org.apache.log4j.Logger;

public class Average extends Configured implements Tool {

    private static final Logger _logger = Logger.getLogger(Average.class);

    private static final String keyspace = "keyspace";
    private static final String inputCF = "inputCF";
    private static final String outputCF = "outputCF";
  
    public static void main(String[] args) throws Exception {
        ToolRunner.run(new Configuration(), new Average(), args);
        System.exit(0);
    }
  
    public int run(String[] args) throws Exception {
        _logger.info("Starting Average");
        ConfigHelper.setRangeBatchSize(getConf(), 99);
    
        final String cassHost = args[0];
        final int numReducers = Integer.parseInt(args[1]);

        //set up job
        _logger.info("Setting up job");
        final Job job = new Job(getConf(), "average");
        final Configuration conf = job.getConfiguration();
        conf.set(keyspace, args[2]);
        conf.set(inputCF, args[3]);
        conf.set(outputCF, args[4]);
    
        _logger.info("Cassandra seed host: " + cassHost);
        _logger.info("Number of reducers: " + numReducers);
        _logger.info("Keyspace: " + conf.get(keyspace));
        _logger.info("Input CF: " + conf.get(inputCF));
        _logger.info("Output CF: " + conf.get(outputCF));
        
        job.setJarByClass(Average.class);
        job.setMapperClass(Map.class);
        job.setNumReduceTasks(numReducers);
    
        //set up cassandra
        _logger.info("Setting up Cassandra");
        ConfigHelper.setInputRpcPort(conf, "9160");
        ConfigHelper.setInputInitialAddress(conf, cassHost);
        ConfigHelper.setInputPartitioner(conf, "org.apache.cassandra.dht.RandomPartitioner");
        ConfigHelper.setInputColumnFamily(conf, conf.get(keyspace), conf.get(inputCF));
        //get all records
        SlicePredicate predicate = new SlicePredicate().setSlice_range(new SliceRange(ByteBufferUtil.bytes(""), ByteBufferUtil.bytes(""), false, Integer.MAX_VALUE));
        ConfigHelper.setInputSlicePredicate(conf, predicate);
    
        ConfigHelper.setOutputInitialAddress(conf, cassHost);
        ConfigHelper.setOutputRpcPort(conf, "9160");
        ConfigHelper.setOutputPartitioner(conf, "org.apache.cassandra.dht.RandomPartitioner");
        ConfigHelper.setOutputColumnFamily(conf, conf.get(keyspace), conf.get(outputCF));
    
        //set up input
        _logger.info("Configuring input");
        job.setInputFormatClass(ColumnFamilyInputFormat.class);
        
        //cass output
        _logger.info("Configuring output");
        job.setReducerClass(Reduce.class);
        job.setOutputFormatClass(ColumnFamilyOutputFormat.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(LongWritable.class);
        job.setOutputKeyClass(ByteBuffer.class);
        job.setOutputValueClass(List.class);
    
        job.waitForCompletion(true);
        return 0;
    }
  
    public static class Map extends Mapper<ByteBuffer, SortedMap<ByteBuffer, IColumn>, Text, LongWritable> {  
    
        public void map(ByteBuffer key, SortedMap<ByteBuffer, IColumn> columns, Context context) throws IOException, InterruptedException { 
      
            for (IColumn col : columns.values())
    	        context.write(new Text(ByteBufferUtil.string(col.name())),
			      new LongWritable(ByteBufferUtil.toLong(col.value())));

        }
    
    }
  
    public static class Reduce extends Reducer<Text, LongWritable, ByteBuffer, List<Mutation>> {
    
        public void reduce(Text key, Iterable<LongWritable> values, Context context) throws IOException, InterruptedException {
            int sum = 0;
            int count = 0;

            for (LongWritable val : values) {
                sum += val.get();
                count++;
            }

            Column c = new Column();
            c.setName(ByteBufferUtil.bytes("Average"));
            c.setValue(ByteBufferUtil.bytes((long)sum/count));
            c.setTimestamp(System.currentTimeMillis());

            Mutation m = new Mutation();
            m.setColumn_or_supercolumn(new ColumnOrSuperColumn());
            m.column_or_supercolumn.setColumn(c);
            context.write(ByteBufferUtil.bytes(key.toString()), Collections.singletonList(m));

        }
    }
}
