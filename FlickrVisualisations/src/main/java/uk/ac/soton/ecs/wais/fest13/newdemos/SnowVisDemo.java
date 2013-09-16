package uk.ac.soton.ecs.wais.fest13.newdemos;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Date;

import javax.sound.midi.MidiUnavailableException;

import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.typography.hershey.HersheyFont;
import org.openimaj.math.geometry.shape.Rectangle;
import org.openimaj.util.data.Context;
import org.openimaj.util.function.Operation;
import org.openimaj.util.stream.Stream;

import uk.ac.soton.ecs.sound.vis.FlickrTagFilter;
import uk.ac.soton.ecs.sound.vis.FlickrTimePostedWindow;
import uk.ac.soton.ecs.sound.vis.FlickrTimePredicate;
import uk.ac.soton.ecs.wais.fest13.FlickrCSVStream;
import uk.ac.soton.ecs.wais.fest13.FlickrCSVStream.FlickrImageSoundOperation;
import uk.ac.soton.ecs.wais.fest13.GetAll;
import uk.ac.soton.ecs.wais.fest13.PassThrough;

@SuppressWarnings("serial")
public class SnowVisDemo extends FlickrVisDemo {

	public SnowVisDemo() throws MidiUnavailableException {
		super("Snow!");
	}
	
	public static void main(String[] args) throws FileNotFoundException, MidiUnavailableException {
		final SnowVisDemo d = new SnowVisDemo();
		
		new FlickrCSVStream(new File(data))
				.filter(new FlickrTimePredicate())
				.transform(new FlickrTimePostedWindow(24 * 60 * 60 * 1000L))
				.forEach(new Operation<Context>() {
					@SuppressWarnings("unchecked")
					@Override
					public void perform(Context object) {
						comments.clear();
						
						((Stream<Context>)object.get("window"))
								.filter(new FlickrTagFilter("snow"))
								.filter(new PassThrough<Context>(heatmapOp))
								.filter(new PassThrough<Context>(imagePointOp))
								.filter(new PassThrough<Context>(new FlickrImageSoundOperation(comments)))
								.forEach(new GetAll<Context>());
						
						d.backgroundMusic.translate(comments, userInformation);
						
						if(d.worldmap != null)
							img.drawImage(d.worldmap, 0, 0);
						else
							img.fill(RGBColour.BLACK);
						
						if(heatmapOp != null)
							heatmapOp.windowDrawn(object);
						
						if(imagePointOp != null)
							imagePointOp.windowDrawn(object);
						
						img.drawShapeFilled(new Rectangle(0, img.getHeight() - 40, img.getWidth(), 40), RGBColour.BLACK);
						img.drawText(df.format(new Date((Long) object.get("start"))), 0, img.getHeight(),
								HersheyFont.ROMAN_SIMPLEX, 18, RGBColour.WHITE);
						
						d.display(img);
						
						try {
							Thread.sleep(1000L / 30L);
						} catch(final InterruptedException e) {
							e.printStackTrace();
						}
					}
				});
	}
}
