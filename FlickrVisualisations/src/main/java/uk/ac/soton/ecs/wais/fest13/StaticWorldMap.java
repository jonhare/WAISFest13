package uk.ac.soton.ecs.wais.fest13;

import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.vis.general.DotPlotVisualisation;
import org.openimaj.vis.general.DotPlotVisualisation.ColouredDot;
import org.openimaj.vis.world.WorldMap;

public class StaticWorldMap
{
	public static MBFImage getMap(int width, int height) {
		return getMap(width, height, RGBColour.WHITE, RGBColour.WHITE, RGBColour.BLACK);
	}
	
	public static MBFImage getMap(int width, int height, Float[] seaColour, Float[] landColour, Float[] outlineColour) {
		final WorldMap<ColouredDot> wp = new WorldMap<ColouredDot>(width, height, new DotPlotVisualisation());
		
		wp.getAxesRenderer().setDrawXAxis(false);
		wp.getAxesRenderer().setDrawYAxis(false);
		wp.getAxesRenderer().setAxisPaddingBottom( 0 );
		wp.getAxesRenderer().setAxisPaddingTop( 0 );
		wp.getAxesRenderer().setAxisPaddingLeft( 0 );
		wp.getAxesRenderer().setAxisPaddingRight( 0 );
		
		wp.setDefaultCountryLandColour(landColour);
		wp.setSeaColour(seaColour);
		wp.setDefaultCountryOutlineColour(outlineColour);
		
		wp.updateVis();
		return wp.getVisualisationImage();
	}
	
	public static void main(String[] args) {
		DisplayUtilities.display(getMap(800, 600));
	}
}
