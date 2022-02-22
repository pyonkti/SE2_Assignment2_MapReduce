package solutions.assignment1;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Mapper.Context;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

import examples.MapRedFileUtils;
import examples.wordcount.MapRedLetterFrequencyCount.MapRecords;
import examples.wordcount.MapRedLetterFrequencyCount.ReduceRecords;

public class MapRedSolution1
{
	public static class MapRecords extends Mapper<LongWritable, Text, Text, LongWritable>
    {
		private final static LongWritable one = new LongWritable(1);
        private Text url = new Text();
        
        @Override
        protected void map(LongWritable key, Text value, Context context)
            throws IOException, InterruptedException
        {
            StringTokenizer itr = new StringTokenizer(value.toString(),"\n");
            while (itr.hasMoreTokens())
            {
         	   Pattern pattern = Pattern.compile("\".*?\"");
         	   Matcher matcher = pattern.matcher(itr.nextToken());
         	   if(matcher.find()) {
         		   StringTokenizer itr2 = new StringTokenizer(matcher.group(0));
         		   int i=0;
         		   while (itr2.hasMoreTokens())
                    {
         			   String str = itr2.nextToken();
         			   if (i == 1) {        				           				   
         				  if(!str.matches("http(.*)")) {
         					 String header = "http://localhost";
            				 str = header.concat(str);
         				  }    
         				  url.set(str);
         				  context.write(url, one);
         				  break;
         			   }
         			   i++;
                    }
         	   }               
            }
        }             
    }
	
    public static class ReduceRecords extends Reducer<Text, LongWritable, Text, LongWritable>
    {
        private LongWritable result = new LongWritable();

        @Override
        protected void reduce(Text key, Iterable<LongWritable> values,
            Context context) throws IOException, InterruptedException
        {
            int sum = 0;
        
            for (LongWritable val : values)
            sum += val.get();
            
            result.set(sum);
            context.write(key, result);
        }
    }
	
    public static void main(String[] args) throws Exception
    {
        Configuration conf = new Configuration();

        String[] otherArgs =
            new GenericOptionsParser(conf, args).getRemainingArgs();
        
        if (otherArgs.length != 2)
        {
            System.err.println("Usage: MapRedSolution1 <in> <out>");
            System.exit(2);
        }
        
        Job job = Job.getInstance(conf, "MapRed Solution #1");
        
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
        
        String[] validMd5Sums = {"af174f7148177b6ad68b7092bc9789f9","878ed82c762143aaf939797f6b02e781"};
        
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
