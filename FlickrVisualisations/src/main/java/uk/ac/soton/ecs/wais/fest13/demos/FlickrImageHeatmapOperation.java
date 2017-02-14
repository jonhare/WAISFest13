package uk.ac.soton.ecs.wais.fest13.demos;

import org.openimaj.image.FImage;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.ColourMap;
import org.openimaj.image.processing.convolution.Gaussian2D;
import org.openimaj.util.data.Context;
import org.openimaj.util.function.Operation;

import uk.ac.soton.ecs.wais.fest13.FlickrCSVStream;

public class FlickrImageHeatmapOperation implements Operation<Context>, WindowProcessListener {

	private static final int GAUSSIANWH = 30;
	private static final int SIGMA = 2;
	private MBFImage img;
	private FImage hist;
	private FImage gaussian;

	public FlickrImageHeatmapOperation(MBFImage img) {
		this.img = img;
		this.hist = img.getBand(0).clone();
		this.gaussian = Gaussian2D.createKernelImage(GAUSSIANWH, GAUSSIANWH, SIGMA);
		this.gaussian.normalise();
	}

	@Override
	public void perform(Context ctx) {
		final double x = (Double) ctx.get(FlickrCSVStream.LONGITUDE) + 180;
		final double y = 90 - (Double) ctx.get(FlickrCSVStream.LATITUDE);

		final int centerx = (int) (x * (1.0 * img.getWidth() / 360));
		final int centery = (int) (y * (1.0 * (img.getHeight() - 40) / 180));
		for (int i = 0; i < GAUSSIANWH; i++) {
			for (int j = 0; j < GAUSSIANWH; j++) {
				//
				final int xx = centerx + i - GAUSSIANWH / 2;
				final int yy = centery + j - GAUSSIANWH / 2;

				if (xx >= 0 && xx < img.getWidth() && yy >= 0 && yy < img.getHeight()) {
					hist.pixels[yy][xx] += this.gaussian.pixels[i][j];
				}
			}
		}
	}

	@Override
	public void windowDrawn(Context window) {
		img.addInplace(ColourMap.Hot.apply(this.hist.clone()));
		hist.multiplyInplace(0.95f);
	}

}
