package uk.ac.soton.ecs.wais.fest13.demos;

import gnu.trove.map.hash.TLongIntHashMap;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.sound.midi.MidiUnavailableException;
import javax.swing.JFrame;

import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.math.geometry.point.Point2dImpl;
import org.openimaj.util.data.Context;
import org.openimaj.util.function.Operation;
import org.openimaj.util.stream.Stream;

import uk.ac.soton.ecs.sound.vis.FlickrTagFilter;
import uk.ac.soton.ecs.sound.vis.FlickrTimePostedWindow;
import uk.ac.soton.ecs.sound.vis.FlickrTimePredicate;
import uk.ac.soton.ecs.wais.fest13.FlickrCSVStream;
import uk.ac.soton.ecs.wais.fest13.FullScreenDemo;

public class ColourMap {
	public static void main(String[] args) throws MidiUnavailableException, IOException {
		final MBFImage img = FullScreenDemo.createImage();
		final JFrame wind = FullScreenDemo.display(img, "Snow Music");

		final String data = "/Users/jon/Data/data-takensort.csv";
		final FlickrImageDrawOperation imagePointOp = new FlickrImageDrawOperation(img, RGBColour.YELLOW);
		imagePointOp.damp = 0.99f;

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

		// final HashMap<Long, Integer> colMap = IOUtils.readFromFile(new
		// File("/Users/jsh2/Data/dominantColours.bin"));
		final TLongIntHashMap colMap = new TLongIntHashMap();
		final DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(
				"/Users/jsh2/Data/dominantColours.bin")));
		try {
			while (true) {
				colMap.put(dis.readLong(), dis.readInt());
			}
		} catch (final EOFException e) {
		}

		new FlickrCSVStream(new File(data))
				.filter(new FlickrTimePredicate())
				.transform(new FlickrTimePostedWindow(24 * 60 * 60 * 1000L))
				.forEach(new Operation<Context>() {
					@SuppressWarnings("unchecked")
					@Override
					public void perform(Context object) {
						img.fill(RGBColour.BLACK);

						((Stream<Context>) object.get("window"))
								.filter(new FlickrTagFilter("snow"))
								.forEach(new Operation<Context>() {
									@Override
									public void perform(Context ctx) {
										final long fid = (Long) ctx.get(FlickrCSVStream.FLICKR_ID);
										if (!colMap.contains(fid))
											return;
										final int colIdx = colMap.get(fid);

										final double x = (Double) ctx.get(FlickrCSVStream.LONGITUDE) + 180;
										final double y = 90 - (Double) ctx.get(FlickrCSVStream.LATITUDE);

										final int xx = (int) (x * (1.0 * img.getWidth() / 360)) - 1;
										final int yy = (int) (y * (1.0 * (img.getHeight() - 40) / 180)) - 1;

										if (xx >= 0 && xx < img.getWidth() && yy >= 0 && yy < img.getHeight()) {
											imagePointOp.layer.drawPoint(new Point2dImpl(xx, yy), cols[colIdx], 5);

										}
									}
								});

						imagePointOp.windowDrawn(object);

						FullScreenDemo.update(wind, img);
						try {
							Thread.sleep(1000L / 30L);
						} catch (final InterruptedException e) {
							e.printStackTrace();
						}
					}
				});
	}
}
