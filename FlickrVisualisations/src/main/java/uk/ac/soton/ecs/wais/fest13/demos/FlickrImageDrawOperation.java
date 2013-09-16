package uk.ac.soton.ecs.wais.fest13.demos;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.typography.hershey.HersheyFont;
import org.openimaj.math.geometry.point.Point2dImpl;
import org.openimaj.math.geometry.shape.Rectangle;
import org.openimaj.util.data.Context;
import org.openimaj.util.function.Operation;

import uk.ac.soton.ecs.wais.fest13.FlickrCSVStream;

public final class FlickrImageDrawOperation implements Operation<Context>, WindowProcessListener
{
	final MBFImage img;
	final Float[] colour;
	SimpleDateFormat df;
	MBFImage layer;
	int size = 3;
	float damp = 0.95f;

	public FlickrImageDrawOperation(MBFImage img, Float[] colour) {
		this.img = img;
		this.layer = img.clone();
		this.colour = colour;
		this.df = new SimpleDateFormat("yyyy-MM-dd");
	}

	@Override
	public void perform(Context ctx) {
		final double x = (Double) ctx.get(FlickrCSVStream.LONGITUDE) + 180;
		final double y = 90 - (Double) ctx.get(FlickrCSVStream.LATITUDE);

		final int xx = (int) (x * (1.0 * img.getWidth() / 360)) - 1;
		final int yy = (int) (y * (1.0 * (img.getHeight() - 40) / 180)) - 1;

		if (xx >= 0 && xx < img.getWidth() && yy >= 0 && yy < img.getHeight()) {
			this.layer.drawPoint(new Point2dImpl(xx, yy), colour, size);

		}
	}

	@Override
	public void windowDrawn(Context object) {
		layer.multiplyInplace(damp);
		layer.drawShapeFilled(new Rectangle(0, img.getHeight() - 40, img.getWidth(), 40), RGBColour.BLACK);
		layer.drawText(df.format(new Date((Long) object.get("start"))), 0, img.getHeight(),
				HersheyFont.ROMAN_SIMPLEX, 18, RGBColour.WHITE);
		img.addInplace(layer);
	}
}
