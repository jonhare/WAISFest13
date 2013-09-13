package uk.ac.soton.ecs.sound.vis;

import java.util.HashSet;
import java.util.regex.Pattern;

import org.openimaj.util.data.Context;
import org.openimaj.util.function.Predicate;

public class FlickrTagFilter implements Predicate<Context> {
	// [tags, dateTaken, flickrId, userId, longitude, latitude, dateUploaded,
	// url]

	private HashSet<Pattern> filterHash;

	public FlickrTagFilter(String... filter) {
		this.filterHash = new HashSet<Pattern>();
		for (final String string : filter) {
			filterHash.add(Pattern.compile(string));
		}
	}

	@Override
	public boolean test(Context object) {
		final String[] tags = (String[]) object.get("tags");
		for (final String string : tags) {
			for (Pattern pat : this.filterHash) {
				if(pat.matcher(string).matches())return true;
			}
		}
		return false;
	}

}
