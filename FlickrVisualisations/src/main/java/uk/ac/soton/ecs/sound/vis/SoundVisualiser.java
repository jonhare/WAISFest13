package uk.ac.soton.ecs.sound.vis;

import java.io.File;
import java.io.FileNotFoundException;

import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.FImage;
import org.openimaj.util.data.Context;
import org.openimaj.util.function.Predicate;
import org.openimaj.util.pair.Pair;
import org.openimaj.util.stream.Stream;

import uk.ac.soton.ecs.wais.fest13.FlickrCSVStream;

public class SoundVisualiser {
	private static final String FLICKR_CSV_SOURCE = "/Users/ss/Development/java/WAISFest13/data/data-10000.csv";

	public static void main(String[] args) throws FileNotFoundException {
		final FImage img = new FImage(1080, 540);
		new FlickrCSVStream(new File(FLICKR_CSV_SOURCE))
		// the tag tfilter
		.filter(new FlickrTagFilter("snow"))
		// render the individual items
//		.filter(new Predicate<Context>() {
//			@Override
//			public boolean test(Context object) {
//				final double x = (Double) object.get(FlickrCSVStream.LONGITUDE) + 180;
//				final double y = 90 - (Double) object.get(FlickrCSVStream.LATITUDE);
//
//				final int xx = (int) (x) * (1 * img.getWidth() / 360);
//				final int yy = (int) (y) * (1 * img.getHeight() / 180);
//
//				if (xx >= 0 && xx < img.getWidth() && yy >= 0 && yy < img.getHeight()) {
//					img.pixels[yy][xx]++;
//					DisplayUtilities.displayName(img, "foo");
//					java.awt.Toolkit.getDefaultToolkit().beep();
//				}
//				return true;
//			}
//		})
		// window by time
		.transform(new FlickrTimePostedWindow(365l* 24l * 60l * 60l * 1000l))
		// decide what sound should be played
		.forEach(new FlickrSoundCreator());
	}

	
}
