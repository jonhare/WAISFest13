package uk.ac.soton.ecs.sound.vis;

import java.util.ArrayList;

import org.openimaj.util.data.Context;
import org.openimaj.util.function.Function;
import org.openimaj.util.stream.AbstractStream;
import org.openimaj.util.stream.Stream;

public class FlickrTimePostedWindow implements Function<Stream<Context>, Stream<Context>> {

	private long windowLength;

	public FlickrTimePostedWindow(long windowLength) {
		this.windowLength = windowLength;
	}

	@Override
	public Stream<Context> apply(final Stream<Context> inner) {
		return new AbstractStream<Context>() {

			@Override
			public boolean hasNext() {
				return inner.hasNext();
			}

			@Override
			public Context next() {
				Context item = inner.next();
				final long currentWindowStartTime = (Long) item.get("dateUploaded");
				final ArrayList<Context> currentWindow = new ArrayList<Context>();
				currentWindow.add(item);
				long end = 0;
				while (inner.hasNext()) {
					item = inner.next();
					if ((Long) item.get("dateUploaded") - currentWindowStartTime >= FlickrTimePostedWindow.this.windowLength)
					{
						end = (Long) item.get("dateUploaded");
						break;
					}
					currentWindow.add(item);
				}
				final Context retcontext = new Context();
				retcontext.put("start", currentWindow);
				retcontext.put("end", end);
				retcontext.put("windowsize", windowLength);
				retcontext.put("window", currentWindow);
				return retcontext;
			}
		};
	}

}
