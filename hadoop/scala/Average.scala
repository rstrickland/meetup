import scala.collection.JavaConversions._
import java.nio.ByteBuffer
import java.util.{SortedMap, Collections}
import org.apache.cassandra.db.IColumn
import org.apache.cassandra.thrift._
import org.apache.cassandra.hadoop._
import org.apache.cassandra.utils.ByteBufferUtil
import org.apache.hadoop.conf._
import org.apache.hadoop.io._
import org.apache.hadoop.mapreduce._
import org.apache.hadoop.util._
import org.apache.log4j.Logger

//only used to pass to JobConf constructor
class Average {}

object Average extends Configured with Tool {

  private val _logger = Logger.getLogger(classOf[Average])

  private val keyspace = "keyspace"
  private val inputCF = "inputCF"
  private val outputCF = "outputCF"
  
  def main(args: Array[String]) {
    ToolRunner.run(new Configuration, Average, args)
    System.exit(0)
  }
  
  def run(args: Array[String]) : Int = {

    _logger.info("Starting Average")
    ConfigHelper.setRangeBatchSize(getConf(), 99)
    
    val cassHost = args(0)
    val numReducers = args(1).toInt

    //set up job
    _logger.info("Setting up job")
    val job = new Job(getConf(), "average")
    val conf = job.getConfiguration

    //we don't have to use conf here since this is only used locally
    //but this shows how to store globally accessible data
    //that we can access at runtime from inside the mapper/reducer
    conf.set(keyspace, args(2))
    conf.set(inputCF, args(3))
    conf.set(outputCF, args(4))
    
    _logger.info("Cassandra seed host: " + cassHost)
    _logger.info("Number of reducers: " + numReducers)
    _logger.info("Keyspace: " + conf.get(keyspace))
    _logger.info("Input CF: " + conf.get(inputCF))
    _logger.info("Output CF: " + conf.get(outputCF))
    
    job.setJarByClass(classOf[Average])    
    job.setMapperClass(classOf[Map])
    job.setNumReduceTasks(numReducers)    
    
    //set up cassandra
    _logger.info("Setting up Cassandra")
    ConfigHelper.setInputRpcPort(conf, "9160")
    ConfigHelper.setInputInitialAddress(conf, cassHost)
    ConfigHelper.setInputPartitioner(conf, "org.apache.cassandra.dht.RandomPartitioner")
    ConfigHelper.setInputColumnFamily(conf, conf.get(keyspace), conf.get(inputCF))
    //get all records
    val predicate = new SlicePredicate().setSlice_range(new SliceRange(ByteBufferUtil.bytes(""), ByteBufferUtil.bytes(""), false, Int.MaxValue))
    ConfigHelper.setInputSlicePredicate(conf, predicate)

    ConfigHelper.setOutputInitialAddress(conf, cassHost)
    ConfigHelper.setOutputRpcPort(conf, "9160")
    ConfigHelper.setOutputPartitioner(conf, "org.apache.cassandra.dht.RandomPartitioner")
    ConfigHelper.setOutputColumnFamily(conf, conf.get(keyspace), conf.get(outputCF))
    
    //set up input
    _logger.info("Configuring input")
    job.setInputFormatClass(classOf[ColumnFamilyInputFormat])
    
    //cass output
    _logger.info("Configuring output")
    job.setReducerClass(classOf[Reduce])
    job.setOutputFormatClass(classOf[ColumnFamilyOutputFormat])
    job.setMapOutputKeyClass(classOf[Text])
    job.setMapOutputValueClass(classOf[LongWritable])
    job.setOutputKeyClass(classOf[ByteBuffer])
    job.setOutputValueClass(classOf[java.util.List[Mutation]])
    
    job.waitForCompletion(true)
    return 0

  }
  
  class Map extends Mapper[ByteBuffer, SortedMap[ByteBuffer, IColumn], Text, LongWritable] {  
    
    //yes this crazy signature is necessary
    //without it Hadoop gets the map and reduce contexts confused
    override def map(key: ByteBuffer, columns: SortedMap[ByteBuffer, IColumn], 
		                 context: Mapper[ByteBuffer, SortedMap[ByteBuffer, IColumn], Text, LongWritable]#Context) { 
      
      //group values by column name
      columns.values.foreach { col => 
        context.write(new Text(ByteBufferUtil.string(col.name)), new LongWritable(ByteBufferUtil.toLong(col.value)))
      }
    }
    
  }
  
  class Reduce extends Reducer[Text, LongWritable, ByteBuffer, java.util.List[Mutation]] {
    
    //see comment on map method above
    //also you must use the fully-qualified java.util.List to avoid confusion with Scala List
    override def reduce(key: Text, values: java.lang.Iterable[LongWritable], 
                        context: Reducer[Text, LongWritable, ByteBuffer, java.util.List[Mutation]]#Context) {

      val vals = values.map(_.get).toList
      
      val c = new Column
      c.setName(ByteBufferUtil.bytes("Average"))
      c.setValue(ByteBufferUtil.bytes(vals.sum/vals.length))
      c.setTimestamp(System.currentTimeMillis)

      val m = new Mutation
      m.setColumn_or_supercolumn(new ColumnOrSuperColumn())
      m.column_or_supercolumn.setColumn(c)

      context.write(ByteBufferUtil.bytes(key.toString), Collections.singletonList(m))
    }
  }
}