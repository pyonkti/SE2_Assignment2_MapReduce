package solutions.assignment2;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

import examples.MapRedFileUtils;

public class MapRedSolution2
{
	public static class MapRecords extends Mapper<LongWritable, Text, Text, LongWritable>
    {
		private final static LongWritable one = new LongWritable(1);
		
		@Override
		protected void map(LongWritable key, Text value, Context context) throws IOException,
		   InterruptedException {		 
		  if (key.get() > 0) {
		   try {
			 String line = value.toString();
		     CSVParser csvParser = new CSVParser(new StringReader(line),CSVFormat.DEFAULT
	                    .withHeader("VendorID","tpep_pickup_datetime","tpep_dropoff_datetime",
	                    		"passenger_count","trip_distance","pickup_longitude",
	                    		"pickup_latitude","RatecodeID","store_and_fwd_flag",
	                    		"dropoff_longitude","dropoff_latitude","payment_type",
	                    		"fare_amount","extra","mta_tax","tip_amount","tolls_amount",
	                    		"improvement_surcharge","total_amount"));
		     for (CSVRecord csvRecord : csvParser) {
	                String pickupTime = csvRecord.get("tpep_pickup_datetime");
	                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH");
	                Date pickupDate = formatter.parse(pickupTime);	         	                
	                String pickupOutputString = new SimpleDateFormat("HH").format(pickupDate);
	                context.write(new Text(pickupOutputString), one);
		     }
		     csvParser.close();
		   } catch (java.text.ParseException e) {}
		  }
		 }
    }
	
    public static class ReduceRecords extends Reducer<Text, LongWritable, Text, 
    LongWritable> {
    	 @Override
    	 protected void reduce(Text key, Iterable<LongWritable> values, Context context)
    	  throws IOException, InterruptedException {
    	  int count = 0;
    	  for (LongWritable value : values) {
    	   count += value.get();
    	  }
    	  context.write(key, new LongWritable(count));
    	 }
    	}
	
    
    public static void main(String[] args) throws Exception
    {
        Configuration conf = new Configuration();

        String[] otherArgs =
            new GenericOptionsParser(conf, args).getRemainingArgs();
        
        if (otherArgs.length != 2)
        {
            System.err.println("Usage: MapRedSolution2 <in> <out>");
            System.exit(2);
        }
        
        Job job = Job.getInstance(conf, "MapRed Solution #2");
        
        job.setMapperClass(MapRecords.class);
        job.setCombinerClass(ReduceRecords.class);
        job.setReducerClass(ReduceRecords.class);
       
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(LongWritable.class);
        
        FileInputFormat.addInputPath(job, new Path(otherArgs[0]));
        FileOutputFormat.setOutputPath(job, new Path(otherArgs[1]));
        
        MapRedFileUtils.deleteDir(otherArgs[1]);
        int exitCode = job.waitForCompletion(true) ? 0 : 1; 

        FileInputStream fileInputStream = new FileInputStream(new File(otherArgs[1]+"/part-r-00000"));
        String md5 = org.apache.commons.codec.digest.DigestUtils.md5Hex(fileInputStream);
        fileInputStream.close();
        
        String[] validMd5Sums = {"03357cb042c12da46dd5f0217509adc8", "ad6697014eba5670f6fc79fbac73cf83", "07f6514a2f48cff8e12fdbc533bc0fe5", 
            "e3c247d186e3f7d7ba5bab626a8474d7", "fce860313d4924130b626806fa9a3826", "cc56d08d719a1401ad2731898c6b82dd", 
            "6cd1ad65c5fd8e54ed83ea59320731e9", "59737bd718c9f38be5354304f5a36466", "7d35ce45afd621e46840627a79f87dac"};
        
        for (String validMd5 : validMd5Sums) 
        {
            if (validMd5.contentEquals(md5))
            {
                System.out.println("The result looks good :-)");
                System.exit(exitCode);
            }
        }
        System.out.println("The result does not look like what we expected :-(");
        System.exit(exitCode);
    }
}
