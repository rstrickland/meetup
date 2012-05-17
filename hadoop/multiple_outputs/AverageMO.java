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
import org.apache.hadoop.mapreduce.lib.output.MultipleOutputs;
import org.apache.hadoop.util.*;
import org.apache.log4j.Logger;

public class AverageMO extends Configured implements Tool {

    private static final Logger _logger = Logger.getLogger(AverageMO.class);

    private static final String keyspace = "keyspace";
    private static final String inputCF = "inputCF";
    private static final String outputCF1 = "outputCF1";
    private static final String outputCF2 = "outputCF2";
  
    public static void main(String[] args) throws Exception {
        ToolRunner.run(new Configuration(), new AverageMO(), args);
        System.exit(0);
    }
  
    public int run(String[] args) throws Exception {

        _logger.info("Starting Average");
        ConfigHelper.setRangeBatchSize(getConf(), 99);
    
        final String cassHost = args[0];
        final int numReducers = Integer.parseInt(args[1]);

        //set up job
        _logger.info("Setting up job");
        final Job job = new Job(getConf(), "average_mo");
        final Configuration conf = job.getConfiguration();

        conf.set(keyspace, args[2]);
        conf.set(inputCF, args[3]);
        conf.set(outputCF1, args[4]);
        conf.set(outputCF2, args[5]);
    
        _logger.info("Cassandra seed host: " + cassHost);
        _logger.info("Number of reducers: " + numReducers);
        _logger.info("Keyspace: " + conf.get(keyspace));
        _logger.info("Input CF: " + conf.get(inputCF));
        _logger.info("Output CF 1: " + conf.get(outputCF1));
        _logger.info("Output CF 2: " + conf.get(outputCF2));
        
        job.setJarByClass(AverageMO.class);
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
        ConfigHelper.setOutputKeyspace(conf, conf.get(keyspace));

        MultipleOutputs.addNamedOutput(job, conf.get(outputCF1), ColumnFamilyOutputFormat.class, ByteBuffer.class, List.class);
        MultipleOutputs.addNamedOutput(job, conf.get(outputCF2), ColumnFamilyOutputFormat.class, ByteBuffer.class, List.class);
    
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
      
            //group values by column name
            for (IColumn col : columns.values())
    	        context.write(new Text(ByteBufferUtil.string(col.name())),
			      new LongWritable(ByteBufferUtil.toLong(col.value())));

        }
    
    }
  
    public static class Reduce extends Reducer<Text, LongWritable, ByteBuffer, List<Mutation>> {

        private MultipleOutputs _output;

        public void setup(Context context) {
            _output = new MultipleOutputs(context);
        }

        public void cleanup(Context context) throws IOException, InterruptedException {
            _output.close();
        }
    
        public void reduce(Text key, Iterable<LongWritable> values, Context context) throws IOException, InterruptedException {

            Configuration conf = context.getConfiguration();

            int sum = 0;
            int count = 0;

            for (LongWritable val : values) {
                sum += val.get();
                count++;
            }

            Column c = new Column();
            c.setName(ByteBufferUtil.bytes("MO_Average"));
            c.setValue(ByteBufferUtil.bytes((long)sum/count));
            c.setTimestamp(System.currentTimeMillis());

            Mutation m = new Mutation();
            m.setColumn_or_supercolumn(new ColumnOrSuperColumn());
            m.column_or_supercolumn.setColumn(c);
            _output.write(conf.get(outputCF1), ByteBufferUtil.bytes(key.toString()), Collections.singletonList(m));

            Column c2 = new Column();
            c2.setName(ByteBufferUtil.bytes("MO_Count"));
            c2.setValue(ByteBufferUtil.bytes((long)count));
            c2.setTimestamp(System.currentTimeMillis());

            Mutation m2 = new Mutation();
            m2.setColumn_or_supercolumn(new ColumnOrSuperColumn());
            m2.column_or_supercolumn.setColumn(c2);
            _output.write(conf.get(outputCF2), ByteBufferUtil.bytes(key.toString()), Collections.singletonList(m2));


        }
    }
}
