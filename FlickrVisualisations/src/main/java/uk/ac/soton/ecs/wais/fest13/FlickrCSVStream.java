package uk.ac.soton.ecs.wais.fest13;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;

import javax.swing.JFrame;

import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.FImage;
import org.openimaj.image.typography.hershey.HersheyFont;
import org.openimaj.math.geometry.point.Point2dImpl;
import org.openimaj.math.geometry.shape.Rectangle;
import org.openimaj.util.data.Context;
import org.openimaj.util.function.Operation;
import org.openimaj.util.stream.AbstractStream;

import uk.ac.soton.ecs.sound.vis.FlickrTimePostedWindow;

public class FlickrCSVStream extends AbstractStream<Context> {
	public static final String FLICKR_ID = "flickrId";
	public static final String USER_ID = "userId";
	public static final String URL = "url";
	public static final String TAGS = "tags";
	public static final String DATE_UPLOADED = "dateUploaded";
	public static final String DATE_TAKEN = "dateTaken";
	public static final String LATITUDE = "latitude";
	public static final String LONGITUDE = "longitude";

	private final static String CSV_REGEX = ",(?=(?:[^\"]*\"[^\"]*\")*(?![^\"]*\"))";

	private BufferedReader reader;
	private String nextLine = null;

	public FlickrCSVStream(File file) throws FileNotFoundException {
		reader = new BufferedReader(new FileReader(file));
	}

	@Override
	public boolean hasNext() {
		if (nextLine != null)
			return true;

		try {
			nextLine = reader.readLine();
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}

		return nextLine != null;
	}

	@Override
	public Context next() {
		if (hasNext()) {
			final Context ret = createContext();
			nextLine = null;
			return ret;
		} else {
			throw new NoSuchElementException();
		}
	}

	private Context createContext() {
		final String[] parts = nextLine.split(CSV_REGEX);

		final Context ctx = new Context();
		ctx.put(FLICKR_ID, parts[0]);
		ctx.put(USER_ID, parts[2]);
		ctx.put(URL, parts[3]);
		ctx.put(TAGS, parts[4].replaceAll("\"", "").split(" "));
		ctx.put(DATE_TAKEN, Long.parseLong(parts[5]));
		ctx.put(DATE_UPLOADED, Long.parseLong(parts[6]));
		ctx.put(LATITUDE, Double.parseDouble(parts[9]));
		ctx.put(LONGITUDE, Double.parseDouble(parts[10]));

		return ctx;
	}

	public static void main(String[] args) throws FileNotFoundException {
		final FImage img = new FImage(1080, 580);

		final JFrame wind = DisplayUtilities.displaySimple(img);

		final String data = "/home/dd/data-takensort.csv";
		// String data =
		// "/Users/ss/Development/java/WAISFest13/data/data-10000.csv";
		new FlickrCSVStream(new File(data))
				.transform(new FlickrTimePostedWindow(24 * 60 * 60 * 1000L))
				// .filter(new FlickrTagFilter("snow"))
				.forEach(new Operation<Context>() {
					@SuppressWarnings("unchecked")
					@Override
					public void perform(Context object) {
						img.multiplyInplace(0.99f);

						for (final Context ctx : (List<Context>) object.get("window")) {
							final double x = (Double) ctx.get(LONGITUDE) + 180;
							final double y = 90 - (Double) ctx.get(LATITUDE);

							final int xx = (int) (x * (1.0 * img.getWidth() / 360));
							final int yy = (int) (y * (1.0 * (img.getHeight() - 40) / 180));

							if (xx >= 0 && xx < img.getWidth() && yy >= 0 && yy < img.getHeight()) {
								img.pixels[yy][xx] = 1;
								img.drawPoint(new Point2dImpl(xx, yy), 1f, 3);
							}
						}
						img.drawShapeFilled(new Rectangle(0, 540, 1080, 40), 0f);
						img.drawText("" + new Date((Long) object.get("start")), 0, 580,
								HersheyFont.TIMES_MEDIUM, 18, 1f);
						DisplayUtilities.display(img, wind);
					}
				});
	}
}
