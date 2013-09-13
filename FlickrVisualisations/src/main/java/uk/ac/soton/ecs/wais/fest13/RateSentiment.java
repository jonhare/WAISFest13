package uk.ac.soton.ecs.wais.fest13; 
import java.io.*;
import java.util.*;

import org.openimaj.util.data.Context;
import org.openimaj.util.function.Operation;

public class RateSentiment
{
	HashMap<String, Integer> rating; 
	InputStream compareWith = RateSentiment.class.getResourceAsStream("/Ratings");
	
	public int median;
	public double mean;
	public HashMap<String, Integer> scores;	
	
	public RateSentiment() 
	{
		try {
			hashmap();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public void hashmap() throws Exception
	{
		BufferedReader br = new BufferedReader(new InputStreamReader(compareWith));
		
		rating = new HashMap<String, Integer>();
		
		String data;
		while((data = br.readLine())!=null)
		{
			rating.put(data.split("	")[0], Integer.parseInt(data.split("	")[1]));
		}
		
		br.close();
	}	
	
	public void calculate(String[] tags)
	{			
		int num=0, sum=0;
		
		scores = new HashMap<String, Integer>();
		
		for(int i=0; i<tags.length; i++)
		{
			if(rating.containsKey(tags[i]))
			{
				scores.put(tags[i], rating.get(tags[i]));
				sum+=rating.get(tags[i]);
				num++;
			}
		}
		if(num == 0){
			mean = 0;
			median = 0;
			return;
		}
		mean = sum/num;
		int[] intarr = toNative(scores.values());
		Arrays.sort(intarr);
		median = intarr[intarr.length/2];		
	}
	
	private int[] toNative(Collection<Integer> values) {
		int[] ret = new int[values.size()];
		int i =0;
		for (int v : values) {
			ret[i] = v;
			i++;
		}
		return ret;
	}

	public double getMean()
	{
		return mean;
	}
	
	public int median()
	{
		return median;
	}
	
	public HashMap<String,Integer> scoreList()
	{
		return scores;
	}
	
	@Override
	public String toString() {
		return String.format("Mean: %2.2f, Median: %d",mean,median);
	}
	
	public static void main(String[] args) throws Exception {
		String data = "/Users/ss/Development/java/WAISFest13/data-taken.csv";
		final RateSentiment sent = new RateSentiment();
		new FlickrCSVStream(new File(data)).forEach(new Operation<Context>() {
			
			@Override
			public void perform(Context object) {
				String[] tags = object.getTyped(FlickrCSVStream.TAGS);
				sent.calculate(tags);
				if(sent.mean!=0)
				{
					System.out.println(Arrays.toString(tags) + ": " + sent);
				}
			}
		});;
	}
}
