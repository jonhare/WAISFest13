package uk.ac.soton.ecs.wais.fest13;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.NoSuchElementException;

import org.openimaj.util.data.Context;
import org.openimaj.util.stream.AbstractStream;

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
		ctx.put(DATE_TAKEN, parts[5]);
		ctx.put(DATE_UPLOADED, parts[6]);
		ctx.put(LATITUDE, parts[9]);
		ctx.put(LONGITUDE, parts[10]);

		return ctx;
	}
}
