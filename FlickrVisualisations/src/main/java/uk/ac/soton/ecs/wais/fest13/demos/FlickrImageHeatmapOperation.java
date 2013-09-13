package uk.ac.soton.ecs.wais.fest13.demos;

import org.openimaj.image.FImage;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.ColourMap;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.math.geometry.point.Point2dImpl;
import org.openimaj.util.data.Context;
import org.openimaj.util.function.Operation;

import uk.ac.soton.ecs.wais.fest13.FlickrCSVStream;

public class FlickrImageHeatmapOperation implements Operation<Context>, WindowProcessListener {

	private MBFImage img;
	private FImage hist;

	public FlickrImageHeatmapOperation(MBFImage img) {
		this.img = img;
		this.hist = img.getBand(0).clone();
	}

	@Override
	public void perform(Context ctx) {
		final double x = (Double) ctx.get(FlickrCSVStream.LONGITUDE) + 180;
		final double y = 90 - (Double) ctx.get(FlickrCSVStream.LATITUDE);

		final int xx = (int) (x * (1.0 * img.getWidth() / 360));
		final int yy = (int) (y * (1.0 * (img.getHeight() - 40) / 180));

		if (xx >= 0 && xx < img.getWidth() && yy >= 0 && yy < img.getHeight()) {
			this.hist.pixels[yy][xx]+=1;
		}
	}

	@Override
	public void windowDrawn(Context window) {
		img.drawImage(ColourMap.Hot.apply(this.hist),0,0);
	}

}
