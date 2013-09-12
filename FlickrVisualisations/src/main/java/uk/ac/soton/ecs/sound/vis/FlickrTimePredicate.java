package uk.ac.soton.ecs.sound.vis;

import org.openimaj.util.data.Context;
import org.openimaj.util.function.Predicate;

import uk.ac.soton.ecs.wais.fest13.FlickrCSVStream;

public class FlickrTimePredicate implements Predicate<Context> {
	@Override
	public boolean test(Context object) {
		if ((Long) object.get(FlickrCSVStream.DATE_TAKEN) < 946706400L)
			return false;
		if ((Long) object.get(FlickrCSVStream.DATE_TAKEN) > System.currentTimeMillis() / 1000L)
			return false;
		return true;
	}
}
