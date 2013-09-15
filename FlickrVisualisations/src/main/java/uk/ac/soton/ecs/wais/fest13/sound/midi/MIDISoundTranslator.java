/**
 * 
 */
package uk.ac.soton.ecs.wais.fest13.sound.midi;

import gnu.trove.list.array.TIntArrayList;

import java.util.Collection;
import java.util.Collections;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Synthesizer;

import org.openimaj.audio.util.WesternScaleNote;

import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoLocation;
import uk.ac.soton.ecs.wais.fest13.SocialComment;
import uk.ac.soton.ecs.wais.fest13.UserInformation;
import uk.ac.soton.ecs.wais.fest13.aggregators.AverageGeoLocation;
import uk.ac.soton.ecs.wais.fest13.aggregators.AverageSentimentAggregator;
import uk.ac.soton.ecs.wais.fest13.sound.SoundTranslator;

/**
 * Translates social commentary into wonderful MIDI music (ahem)
 * 
 * @author David Dupplaw (dpd@ecs.soton.ac.uk)
 * @created 12 Sep 2013
 * @version $Author$, $Revision$, $Date$
 */
public class MIDISoundTranslator implements SoundTranslator
{
	/** The MIDI synth in use */
	private Synthesizer synth;

	/** Next channel on which to play a note */
	private int nextChannel = 0;

	/**
	 * These will be the notes to generate. We'll get the actual notes from the
	 * BackgroundMusic instance if it's switched on. These notes will be used if
	 * the background music is off. They are notes based on the original Panart
	 * hang-drum.
	 */
	private String[] notesToGenerate = new String[] { "D3", "A3", "A#3", "C4", "D4", "E4", "F4" };

	/** The pan controller - to set where in the stereo field a note is played */
	public int PAN_CONTROLLER = 10;

	/** The volume controller - to audibly represent the distance from our user */
	public int VOLUME_CONTROLLER = 7;

	public int CUTOFF_FREQUENCY_CONTROLLER = 74;
	public int FILTER_RESONANCE_CONTROLLER = 71;

	/** Channels not to use for general comment translation (0-indexed) */
	private TIntArrayList reservedChannels = new TIntArrayList(new int[] { 9, 1, 2, 3 });

	/** Aggregator to work out the average geo location */
	private AverageGeoLocation avGeoLocAggregator = new AverageGeoLocation();

	/** The average sentiment aggregator */
	private AverageSentimentAggregator avSentimentAggregator = new AverageSentimentAggregator();

	/** Whether to generate background tracks */
	private boolean useBackground = false;

	/**
	 * A memory of which notes are on which channels (to turn them off before we
	 * change the sound or change the channel setup.
	 */
	private int[] notesOn = new int[16];

	/** The sequencer to use to run the background music */
	private Sequencer sequencer;

	/** The background music instance */
	private BackgroundMusic backgroundMusic;

	/** The number of social comments since the last time we checked */
	private long countSinceLast = 0;

	/** How much the mood needs to change */
	private int currentMoodAcceleration = 0;

	/** Instruments to change mood - from negative to positive */
	private int[] moodInstruments = new int[] {
			MIDIInstruments.BARITONE_SAX,
			MIDIInstruments.ACOUSTIC_GUITAR_NYLON,
			MIDIInstruments.KALIMBA,
			MIDIInstruments.ACOUSTIC_GRAND_PIANO, // Middle
			MIDIInstruments.ACOUSTIC_GUITAR_STEEL,
			MIDIInstruments.FX_3_CRYSTAL,
			MIDIInstruments.FX_5_BRIGHTNESS,
	};

	/**
	 * Default constructor
	 * 
	 * @throws MidiUnavailableException
	 *             If a synth could not be created
	 */
	public MIDISoundTranslator() throws MidiUnavailableException
	{
		// Open the synthesizer to play the music
		this.synth = MidiSystem.getSynthesizer();
		synth.open();

		// Setup the sequencer for the background music, if we're going
		// to use it.
		if (useBackground)
		{
			this.sequencer = MidiSystem.getSequencer();
			if (sequencer != null)
			{
				sequencer.open();

				try
				{
					this.backgroundMusic = new BackgroundMusic();

					// Set up the sequencer.
					sequencer.setSequence(backgroundMusic.getSequence());
					sequencer.setTempoInBPM(100);

					// Set the mood to the default mood
					backgroundMusic.setMood(
							backgroundMusic.getCurrentMood(), sequencer);

					// We handle looping ourselves so that we can alter the
					// mood on each loop.
					sequencer.addMetaEventListener(new MetaEventListener()
					{
						@Override
						public void meta(MetaMessage meta)
						{
							// Alter the mood. Calculate the acceleration of the
							// mood
							currentMoodAcceleration += (int) ((countSinceLast - 100) / 50);
							currentMoodAcceleration = (int) Math.signum(currentMoodAcceleration);
							System.out.println(countSinceLast + " social comments -> " + currentMoodAcceleration);

							// Change mood
							backgroundMusic.setMood(
									backgroundMusic.getCurrentMood()
											+ currentMoodAcceleration, sequencer);

							notesToGenerate = backgroundMusic.getNotesToFitMood(backgroundMusic.getCurrentMood());

							// Loop continuously.
							countSinceLast = 0;
							sequencer.setTickPosition(0);
							sequencer.start();
						}
					});

					sequencer.start();
				} catch (final InvalidMidiDataException e)
				{
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see uk.ac.soton.ecs.wais.fest13.sound.SoundTranslator#translate(java.util.Collection,
	 *      uk.ac.soton.ecs.wais.fest13.UserInformation)
	 */
	@Override
	public void translate(Collection<SocialComment> comment, UserInformation userInformation)
	{
		if (comment.size() == 0)
			return;
		countSinceLast += comment.size();

		// The average geo location of all the comments
		final GeoLocation gl = avGeoLocAggregator.aggregate(comment, userInformation);

		// The average sentiment of all the comments
		double sentimentScore = avSentimentAggregator.aggregate(comment, userInformation);
		sentimentScore = (int) (64 + (sentimentScore * 64));
		System.out.println("Sentiment: " + sentimentScore);

		// Give a bit more range to the notes we've provided
		final int alterOctave = (int) (Math.random() * 3) - 1;

		// Create the note
		final int indx = (int) (Math.random() * notesToGenerate.length);
		final WesternScaleNote note = WesternScaleNote.createNote(
				notesToGenerate[indx]);
		note.noteNumber += 12 * alterOctave;

		// Get the channel
		final MidiChannel chan = synth.getChannels()[nextChannel];

		// Choose an appropriate instrument based on sentiment.
		final int prog = moodInstruments[(int) Math.max(0, Math.min(
				moodInstruments.length - 1, sentimentScore * moodInstruments.length / 127))];
		System.out.println("Program: " + prog);
		chan.programChange(prog);

		// Set the pan position
		chan.controlChange(PAN_CONTROLLER,
				getPanPosition(gl.longitude));

		// Set the volume based on the distance from the observer
		chan.controlChange(VOLUME_CONTROLLER,
				getUserDistanceVolume(gl, userInformation.location));

		// Set the brightness of the sound based on the sentiment score
		chan.controlChange(FILTER_RESONANCE_CONTROLLER, 40);
		chan.controlChange(CUTOFF_FREQUENCY_CONTROLLER, (int) sentimentScore);

		// Stop the previous note on this channel
		chan.noteOff(notesOn[nextChannel]);

		// Play the note
		chan.noteOn(note.noteNumber, 100);
		notesOn[nextChannel] = note.noteNumber;

		// Increment the channel to the next one.
		nextChannel++;
		nextChannel %= 16;

		// Check if it's a reserved channel
		while (reservedChannels.contains(nextChannel))
		{
			nextChannel++;
			nextChannel %= 16;
		}
	}

	/**
	 * Converts -180 to +180 longitude to a pan position 0 - 127
	 * 
	 * @param longitude
	 *            -180 to 180
	 * @return pan position 0 to 127
	 */
	public int getPanPosition(double longitude)
	{
		return (int) ((longitude + 180) * 127 / 360);
	}

	/**
	 * Converts the user's distance into a volume value 0 - 127
	 * 
	 * @param comment
	 *            The comment geo location
	 * @param userLocation
	 *            The user geo location
	 * @return The volume
	 */
	public int getUserDistanceVolume(GeoLocation comment, GeoLocation userLocation)
	{
		// This distance calculation is ported from:
		// http://www.movable-type.co.uk/scripts/latlong.html
		final double R = 6371; // km
		final double dLat = Math.toRadians(userLocation.latitude - comment.latitude);
		final double dLon = Math.toRadians(userLocation.longitude - comment.longitude);
		final double lat1 = Math.toRadians(comment.latitude);
		final double lat2 = Math.toRadians(userLocation.latitude);

		final double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
				Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2);
		final double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		final double d = R * c;

		// The maximum great circle distance is just over 20000km, so we'll
		// divide by that and multiply by the maximum our volume can be. In this
		// case to avoid completely inaudible updates, our minimum will be
		// 27, so range = 100
		final int volume = 127 - ((int) (d * 100 / 20020) + 27);

		return volume;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see uk.ac.soton.ecs.wais.fest13.sound.SoundTranslator#stop()
	 */
	@Override
	public void stop()
	{
		this.synth.close();
	}

	/**
	 * @param args
	 * @throws MidiUnavailableException
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws MidiUnavailableException, InterruptedException
	{
		// Put our pretend user in London
		final UserInformation userInformation = new UserInformation();
		userInformation.location = new GeoLocation(51.507222, -0.1275);

		// Create 20 random social comments
		final MIDISoundTranslator mst = new MIDISoundTranslator();
		for (int i = 0; i < 36; i++)
		{
			final SocialComment comment = new SocialComment();
			comment.location = new GeoLocation( /* Math.random()*9 */0, i * 10 - 180);

			mst.translate(Collections.singleton(comment), userInformation);
			Thread.sleep(600);
		}

		Thread.sleep(50000);
	}
}
