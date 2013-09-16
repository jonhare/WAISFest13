package uk.ac.soton.ecs.wais.fest13.newdemos;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.sound.midi.MidiUnavailableException;
import javax.swing.JFrame;

import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.ColourSpace;
import org.openimaj.image.colour.RGBColour;

import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoLocation;
import uk.ac.soton.ecs.wais.fest13.FullScreenDemo;
import uk.ac.soton.ecs.wais.fest13.SocialComment;
import uk.ac.soton.ecs.wais.fest13.StaticWorldMap;
import uk.ac.soton.ecs.wais.fest13.UserInformation;
import uk.ac.soton.ecs.wais.fest13.demos.FlickrImageDrawOperation;
import uk.ac.soton.ecs.wais.fest13.demos.FlickrImageHeatmapOperation;
import uk.ac.soton.ecs.wais.fest13.sound.SoundTranslator;
import uk.ac.soton.ecs.wais.fest13.sound.midi.BasicMIDISoundTranslator;
import uk.ac.soton.ecs.wais.fest13.sound.midi.MIDISoundTranslator;

@SuppressWarnings("serial")
public abstract class FlickrVisDemo implements KeyListener {
	protected final static String data = "/Users/jamie/Data/data-taken.csv";

//	protected final static MBFImage img = FullScreenDemo.createImage();
//	protected final static MBFImage img = new MBFImage(1080, 580, ColourSpace.RGB);
	protected final static MBFImage img = new MBFImage(
			GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().width,
			GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().height,
			ColourSpace.RGB);
	protected final static SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
	protected final static List<SocialComment> comments = new ArrayList<SocialComment>();
	protected final static UserInformation userInformation = new UserInformation();
	
	protected MBFImage worldmap;
	protected SoundTranslator backgroundMusic;
	
	protected static FlickrImageHeatmapOperation heatmapOp;
	protected static FlickrImageDrawOperation imagePointOp;
	
	protected JFrame window;
	
	private final String title;
	
	// States
	private boolean windowed;
	private boolean worldmapped;
	private boolean oldmusic;
	private boolean heatmapped;
	private boolean dotted;
	
	public FlickrVisDemo(String title) throws MidiUnavailableException {
		this.title = title;
		
//		windowed = false;
		windowed = true;
		worldmapped = false;
		oldmusic = true;
		heatmapped = false;
		dotted = true;
		
//		window = FullScreenDemo.display(img, title);
		window = DisplayUtilities.displaySimple(img, title);
		backgroundMusic = new BasicMIDISoundTranslator();
		worldmap = genWorldMap();
		heatmapOp = genHeatOp();
		imagePointOp = genDotOp();
		
		userInformation.location = new GeoLocation(51.5, 0);

		window.addKeyListener(this);
	}
	
	private void makeWindowed() {
		GraphicsDevice dev = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		window.setVisible(false);
		window.dispose();
		dev.setFullScreenWindow(null);
		window = DisplayUtilities.displaySimple(img, title);
		window.setVisible(true);
		
		windowed = true;
	}
	
	private void makeFullScreen() {
		window.setVisible(false);
		window.dispose();
		window = FullScreenDemo.display(img, title);
		window.setVisible(true);
		
		windowed = false;
	}
	
	private FlickrImageHeatmapOperation genHeatOp() {
		if(heatmapped) {
			return new FlickrImageHeatmapOperation(img);
		} else {
			return null;
		}
	}
	
	private FlickrImageDrawOperation genDotOp() {
		if(dotted) {
			return new FlickrImageDrawOperation(img, RGBColour.YELLOW);
		} else {
			return null;
		}
	}
	
	private MBFImage genWorldMap() {
		if(worldmapped) {
			return StaticWorldMap.getMap(window.getWidth(), window.getHeight(),
					RGBColour.BLACK,
					RGBColour.BLACK,
					RGBColour.DARK_GRAY);
		}
		return null;
	}
	
	protected void display(MBFImage img) {
		if(windowed)
			DisplayUtilities.display(img, window);
		else
			FullScreenDemo.update(window, img);
	}
	
	@Override
	public void keyTyped(KeyEvent e) {
		try {
			switch(e.getKeyChar()) {
				case 'm':
				case 'M':
					if(oldmusic) {
						backgroundMusic = new MIDISoundTranslator();
					} else {
						backgroundMusic.stop();
						backgroundMusic = new BasicMIDISoundTranslator();
					}
					oldmusic = !oldmusic;
					break;
					
				case 'w':
				case 'W':
					worldmapped = !worldmapped;
					worldmap = genWorldMap();
					break;
				
				case 'h':
				case 'H':
					heatmapped = !heatmapped;
					heatmapOp = genHeatOp();
					break;
				
				case 'd':
				case 'D':
					dotted = !dotted;
					imagePointOp = genDotOp();
					break;
				
				case 'f':
				case 'F':
					windowed = !windowed;
					if(windowed)
						makeWindowed();
					else
						makeFullScreen();
					break;
			}
		} catch(Exception e1) {
			e1.printStackTrace();
		}
	}

	@Override
	public void keyPressed(KeyEvent e) {
	}

	@Override
	public void keyReleased(KeyEvent e) {
	}
}
