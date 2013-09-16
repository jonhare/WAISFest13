package uk.ac.soton.ecs.wais.fest13;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map.Entry;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.Text;
import org.openimaj.hadoop.sequencefile.SequenceFileUtility;
import org.openimaj.hadoop.sequencefile.TextBytesSequenceFileUtility;

public class ExtractHistograms {
	public static void main(String[] args) throws IOException {
		final String dataPath =
				"hdfs://seurat/data/mediaeval/placing/images-rgbhist444-v2.seq/";

		final BufferedWriter bw = new BufferedWriter(new FileWriter("/Users/jsh2/Data/colours.csv"));

		final Path[] paths = SequenceFileUtility.getFilePaths(dataPath, "part-m-");

		for (final Path p : paths) {
			System.err.println(p);
			final TextBytesSequenceFileUtility sf = new TextBytesSequenceFileUtility(p.toString(), true);

			for (final Entry<Text, BytesWritable> entry : sf) {
				final String key = entry.getKey().toString();
				byte[] bytes = entry.getValue().getBytes();
				bytes = Arrays.copyOf(bytes, entry.getValue().getLength());
				String value = new String(bytes);
				value = value.substring(value.indexOf("\n")).replace("\n", "").trim();

				final String[] parts = value.split(" ");
				String val = String.format("%2.5f", Double.parseDouble(parts[0]));
				for (int i = 1; i < parts.length; i++)
					val += String.format(",%2.5f", Double.parseDouble(parts[i]));

				bw.write(key + "," + val + "\n");
			}
		}

		bw.close();
	}
}
