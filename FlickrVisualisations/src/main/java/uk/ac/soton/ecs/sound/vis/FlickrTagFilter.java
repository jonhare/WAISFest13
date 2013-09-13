package uk.ac.soton.ecs.sound.vis;

import java.util.HashSet;

import org.openimaj.util.data.Context;
import org.openimaj.util.function.Predicate;

public class FlickrTagFilter implements Predicate<Context> {
	// [tags, dateTaken, flickrId, userId, longitude, latitude, dateUploaded,
	// url]

	private HashSet<String> filterHash;

	public FlickrTagFilter(String... filter) {
		this.filterHash = new HashSet<String>();
		for (final String string : filter) {
			filterHash.add(string.toLowerCase());
		}
	}

	@Override
	public boolean test(Context object) {
		final String[] tags = (String[]) object.get("tags");
		for (final String string : tags) {
			if (this.filterHash.contains(string.toLowerCase())) {
				return true;
			}
		}
		return false;
	}

}
