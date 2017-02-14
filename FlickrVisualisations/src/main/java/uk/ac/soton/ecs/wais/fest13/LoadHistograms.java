package uk.ac.soton.ecs.wais.fest13;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.openimaj.util.array.ArrayUtils;
import org.openimaj.util.iterator.TextLineIterable;

public class LoadHistograms {
	public static void main(String[] args) throws InterruptedException, IOException {
		final Float[][] cols = new Float[64][3];

		for (int r = 0, i = 0; r < 4; r++) {
			final float red = 0.25f * r + 0.125f;
			for (int g = 0; g < 4; g++) {
				final float green = 0.25f * g + 0.125f;
				for (int b = 0; b < 4; b++, i++) {
					final float blue = 0.25f * b + 0.125f;
					cols[i][0] = red;
					cols[i][1] = green;
					cols[i][2] = blue;
				}
			}
		}

		// final DataOutputStream dos = new DataOutputStream(new
		// BufferedOutputStream(new FileOutputStream(
		// "/Users/jsh2/Data/dominantColours.bin")));
		// final HashMap<Long, Integer> map = new HashMap<Long, Integer>();
		final FileWriter fos = new FileWriter(new File("/Users/jsh2/Data/nyc-dominant-colours.csv"));
		final float[] vec = new float[64];
		for (final String line : new TextLineIterable(new File("/Users/jsh2/Data/nyc-colours.csv"))) {
			final String[] parts = line.split(",");

			final long id = Long.parseLong(parts[0].trim());
			for (int i = 0; i < 64; i++)
				vec[i] = Float.parseFloat(parts[i + 1].trim());

			final int idx = ArrayUtils.maxIndex(vec);

			fos.write(id + ", " + cols[idx][0] + ", " + cols[idx][1] + ", " + cols[idx][2] + "\n");
			// map.put(id, idx);
			// dos.writeLong(id);
			// dos.writeInt(idx);

			// if (map.size() % 1000 == 0)
			// System.out.println(map.size());
		}
		fos.close();
		// dos.close();
		// IOUtils.writeToFile(map, new
		// File("/Users/jsh2/Data/dominantColours.bin"));
	}
}
