package uk.ac.soton.ecs.sound.vis;

import java.util.List;

import org.openimaj.util.data.Context;
import org.openimaj.util.function.Operation;

public class FlickrSoundCreator implements Operation<Context> {

	@Override
	public void perform(Context object) {
		List<Context> x = object.getTyped("window");
		System.out.println(x.size());
	}

}
