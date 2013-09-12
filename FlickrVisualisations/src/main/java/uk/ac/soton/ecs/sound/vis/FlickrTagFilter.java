package uk.ac.soton.ecs.sound.vis;

import java.util.HashSet;

import org.openimaj.util.data.Context;
import org.openimaj.util.function.Predicate;

public class FlickrTagFilter implements Predicate<Context> {
//	[tags, dateTaken, flickrId, userId, longitude, latitude, dateUploaded, url]

	private HashSet<String> filterHash;
	public FlickrTagFilter(String ... filter) {
		this.filterHash = new HashSet<String>();
		for (String string : filter) {
			filterHash.add(string);
		}
	}
	@Override
	public boolean test(Context object) {
		String[] tags = (String[]) object.get("tags");
		for (String string : tags) {
			if(this.filterHash.contains(string)) {
				return true;
			}
		}
		return false;
	}

}
