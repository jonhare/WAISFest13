package uk.ac.soton.ecs.wais.fest13;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.openimaj.io.IOUtils;
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

		final HashMap<Long, Integer> map = new HashMap<Long, Integer>();
		final float[] vec = new float[64];
		for (final String line : new TextLineIterable(new File("/Users/jsh2/Data/colours.csv"))) {
			final String[] parts = line.split(",");

			final long id = Long.parseLong(parts[0]);
			for (int i = 0; i < 64; i++)
				vec[i] = Float.parseFloat(parts[i + 1]);

			final int idx = ArrayUtils.maxIndex(vec);
			map.put(id, idx);

			if (map.size() % 1000 == 0)
				System.out.println(map.size());
		}

		IOUtils.writeToFile(map, new File("/Users/jsh2/Data/dominantColours.bin"));
	}
}
