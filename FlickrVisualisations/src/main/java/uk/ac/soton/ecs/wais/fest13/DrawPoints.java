package uk.ac.soton.ecs.wais.fest13;

import gnu.trove.list.array.TLongArrayList;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.openimaj.content.animation.animator.DoubleArrayValueAnimator;
import org.openimaj.content.animation.animator.LinearDoubleValueAnimator;
import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.FImage;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.ColourSpace;
import org.openimaj.video.xuggle.XuggleVideoWriter;

import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoLocation;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.util.Utils;

public class DrawPoints {
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws IOException {
		final List<GeoLocation> pts = Utils.readLatLng(new File("/Users/jsh2/training_latlng"), new TLongArrayList());

		final double[][] targets = {
				{ 0, 0, 360, 10, 0 },
				{ 40.7833, -73.9667, 0.15, 30, 0 },
				{ 40.7833, -73.9667, 80, 0, 0 },
				{ 48.8611, 2.3364, 0.1, 30, 0 },
				{ 48.8611, 2.3364, 10.0, 0, 0 },
				{ 51.5033, -0.1197, 0.1, 30, 0 },
				{ 51.5033, -0.1197, 3, 0, 0 },
				{ 50.9346, -1.3960, 0.02, 0, 0 }
		};

		final FImage gimg = new FImage(1080, 540);
		final MBFImage img = new MBFImage(1080, 540, ColourSpace.RGB);
		final XuggleVideoWriter writer = new XuggleVideoWriter("/Users/jsh2/earth.mp4", 1080, 540, 30);

		final List<DoubleArrayValueAnimator> animators = new ArrayList<DoubleArrayValueAnimator>();

		for (int i = 1; i < targets.length; i++) {
			animators.add(new DoubleArrayValueAnimator(
					new LinearDoubleValueAnimator((int) targets[i - 1][3], (int) targets[i - 1][4], targets[i - 1][0],
							targets[i][0], 160),
					new LinearDoubleValueAnimator((int) targets[i - 1][3], (int) targets[i - 1][4], targets[i - 1][1],
							targets[i][1], 160),
					new LinearDoubleValueAnimator((int) targets[i - 1][3], (int) targets[i - 1][4], targets[i - 1][2],
							targets[i][2], 200)
					));
		}

		for (final DoubleArrayValueAnimator ani : animators) {
			while (!ani.hasFinished()) {
				final double[] next = ani.nextValue();
				img.fill(new Float[] { 16f / 256f, 23f / 256f, 53f / 256f });
				gimg.fill(1f);

				final double vx = 180 + next[1];
				final double vy = 90 - next[0];
				final double vw = next[2];
				final double sx = 360 / vw;
				final double vh = 180 / sx;

				System.out.format("%4.3f\t%4.3f\t%4.3f\n", next[0], next[1], next[2]);

				for (final GeoLocation pt : pts) {
					final double x = pt.longitude + 180;
					final double y = 90 - pt.latitude;

					final int xx = (int) ((x - (vx - (vw / 2))) * (sx * img.getWidth() / 360));
					final int yy = (int) ((y - (vy - (vh / 2))) * (sx * img.getHeight() / 180));

					if (xx >= 0 && xx < img.getWidth() && yy >= 0 && yy < img.getHeight()) {
						gimg.pixels[yy][xx]++;
						img.setPixel(xx, yy, new Float[] { 241f / 256f, 230f / 256f, 206f / 256f });
					}
				}

				Utils.logNorm(gimg);
				gimg.normalise();
				for (int y = 0; y < 540; y++) {
					for (int x = 0; x < 1080; x++) {
						if (gimg.pixels[y][x] == 0) {
							gimg.pixels[y][x] = 1;
						} else {
							gimg.pixels[y][x] = (float) Math.pow(gimg.pixels[y][x], 1.0 / 2);
						}
					}
				}
				img.multiplyInplace(gimg);

				DisplayUtilities.displayName(img, "Earth");
				writer.addFrame(img);
			}
		}
		writer.close();
	}
}
