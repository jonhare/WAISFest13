/**
 *
 */
package uk.ac.soton.ecs.wais.fest13.sound.midi;

import gnu.trove.list.array.TIntArrayList;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Synthesizer;

import org.openimaj.audio.util.WesternScaleNote;

import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoLocation;
import uk.ac.soton.ecs.wais.fest13.SocialComment;
import uk.ac.soton.ecs.wais.fest13.UserInformation;
import uk.ac.soton.ecs.wais.fest13.aggregators.AverageGeoLocation;
import uk.ac.soton.ecs.wais.fest13.aggregators.AverageSentimentAggregator;
import uk.ac.soton.ecs.wais.fest13.sound.SoundTranslator;

import com.sun.media.sound.SF2Soundbank;

/**
 * 	Translates social commentary into wonderful MIDI music (ahem)
 *
 * 	@author David Dupplaw (dpd@ecs.soton.ac.uk)
 * 	@created 12 Sep 2013
 * 	@version $Author$, $Revision$, $Date$
 */
public class MIDISoundTranslator implements SoundTranslator
{
	/** The MIDI synth in use */
	private final Synthesizer synth;

	/** Next channel on which to play a note */
	private int nextChannel = 0;

	/**
	 * 	These will be the notes to generate. We'll get the actual notes
	 *  from the BackgroundMusic instance if it's switched on.
	 *  These notes will be used if the background music is off. They
	 *  are notes based on the original Panart hang-drum.
	 */
	private String[] notesToGenerate = new String[]{"D3","A3","A#3","C4","D4","E4","F4"};

	/** The pan controller - to set where in the stereo field a note is played */
	public int PAN_CONTROLLER = 10;

	/** The volume controller - to audibly represent the distance from our user */
	public int VOLUME_CONTROLLER = 7;

	public int CUTOFF_FREQUENCY_CONTROLLER = 74;
	public int FILTER_RESONANCE_CONTROLLER = 71;

	/** Channels not to use for general comment translation (0-indexed) */
	private final TIntArrayList reservedChannels = new TIntArrayList( new int[]{9,1,2,3} );

	/** Aggregator to work out the average geo location */
	private final AverageGeoLocation avGeoLocAggregator = new AverageGeoLocation();

	/** The average sentiment aggregator */
	private final AverageSentimentAggregator avSentimentAggregator = new AverageSentimentAggregator();

	/** Whether to generate background tracks */
	private final boolean useBackground = true;

	/**
	 * 	A memory of which notes are on which channels (to turn them off before
	 *  we change the sound or change the channel setup.
	 */
	private final int[] notesOn = new int[16];

	/** The sequencer to use to run the background music */
	private Sequencer sequencer;

	/** The background music instance */
	private BackgroundMusic backgroundMusic;

	/** The number of social comments since the last time we checked */
	private long countSinceLast = 0;

	/** How much the mood needs to change */
	private int currentMoodAcceleration = 0;

	/** Instruments to change mood - from negative to positive */
	private final int[] moodInstruments = new int[]{
			MIDIInstruments.BARITONE_SAX,
			MIDIInstruments.ACOUSTIC_GUITAR_NYLON,
			MIDIInstruments.KALIMBA,
			MIDIInstruments.ACOUSTIC_GRAND_PIANO,	// Middle
			MIDIInstruments.ACOUSTIC_GUITAR_STEEL,
			MIDIInstruments.FX_3_CRYSTAL,
			MIDIInstruments.FX_5_BRIGHTNESS,
	};

	/**
	 * 	Default constructor
	 *	@throws MidiUnavailableException If a synth could not be created
	 */
	public MIDISoundTranslator() throws MidiUnavailableException
	{
		// Open the synthesizer to play the music
		this.synth = MidiSystem.getSynthesizer();
		this.synth.open();

		try
		{
			this.synth.loadAllInstruments( new SF2Soundbank(
				MIDISoundTranslator.class.getResourceAsStream( "/8MBGMSFX.SF2") ) );
		}
		catch( final IOException e1 )
		{
			System.err.println( "Unable to load Soundbank. Will continue with default.");
			e1.printStackTrace();
		}

		// Setup the sequencer for the background music, if we're going
		// to use it.
		if( this.useBackground )
		{
			this.sequencer = MidiSystem.getSequencer();
			if( this.sequencer != null )
			{
				this.sequencer.open();

				try
				{
					this.backgroundMusic = new BackgroundMusic();

					// Set up the sequencer.
					this.sequencer.setSequence( this.backgroundMusic.getSequence() );
					this.sequencer.setTempoInBPM( 100 );

					// Set the mood to the default mood
					this.backgroundMusic.setMood(
						this.backgroundMusic.getCurrentMood(), this.sequencer );

					// The background music can return a new sequence when next() is called
					final Iterator<Sequence> bi = this.backgroundMusic.iterator();

					// We handle looping ourselves so that we can alter the
					// mood on each loop.
					this.sequencer.addMetaEventListener( new MetaEventListener()
					{
						@Override
						public void meta( final MetaMessage meta )
						{
							// Alter the mood. Calculate the acceleration of the mood
							MIDISoundTranslator.this.currentMoodAcceleration += (int)((MIDISoundTranslator.this.countSinceLast-100)/50);
							MIDISoundTranslator.this.currentMoodAcceleration = (int)Math.signum( MIDISoundTranslator.this.currentMoodAcceleration );
							System.out.println( MIDISoundTranslator.this.countSinceLast+" social comments -> "+MIDISoundTranslator.this.currentMoodAcceleration );

							// Change mood
							MIDISoundTranslator.this.backgroundMusic.setMood(
								MIDISoundTranslator.this.backgroundMusic.getCurrentMood()
									+MIDISoundTranslator.this.currentMoodAcceleration, MIDISoundTranslator.this.sequencer );

							MIDISoundTranslator.this.notesToGenerate = MIDISoundTranslator.this.backgroundMusic.getNotesToFitMood( MIDISoundTranslator.this.backgroundMusic.getCurrentMood() );

							// Loop continuously.
							MIDISoundTranslator.this.countSinceLast = 0;

							// Try and get a new sequence
							try
							{
								MIDISoundTranslator.this.sequencer.setSequence( bi.next() );
							}
							catch( final InvalidMidiDataException e )
							{
								e.printStackTrace();
							}

							// Change the speed if things get more active
							MIDISoundTranslator.this.sequencer.setTempoInBPM(
									80 + MIDISoundTranslator.this.backgroundMusic.getCurrentMood()*5 );

							// Restart the sequencer
							MIDISoundTranslator.this.sequencer.setTickPosition(0);
							MIDISoundTranslator.this.sequencer.start();
						}
					} );

					this.sequencer.start();
				}
				catch( final InvalidMidiDataException e )
				{
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 *	{@inheritDoc}
	 * 	@see uk.ac.soton.ecs.wais.fest13.sound.SoundTranslator#translate(java.util.Collection, uk.ac.soton.ecs.wais.fest13.UserInformation)
	 */
	@Override
	public void translate( final Collection<SocialComment> comment, final UserInformation userInformation )
	{
		if( comment.size() == 0 ) return;
		this.countSinceLast += comment.size();

		// The average geo location of all the comments
		final GeoLocation gl = this.avGeoLocAggregator.aggregate( comment, userInformation );

		// The average sentiment of all the comments (0-127 with 64 the middle neutral sentiment)
		double sentimentScore = this.avSentimentAggregator .aggregate( comment, userInformation );
		sentimentScore = (int)(64+(sentimentScore*64));
		System.out.println( "Sentiment: "+sentimentScore );

		// Give a bit more range to the notes we've provided
		final int alterOctave = (int)(Math.random()*3)-1;

		// Create the note
		final int indx = (int)(Math.random() * this.notesToGenerate.length);
		final WesternScaleNote note = WesternScaleNote.createNote(
				this.notesToGenerate[indx] );
		note.noteNumber += 12 * alterOctave;

		// Get the channel
		// System.out.println( nextChannel );
		final MidiChannel chan = this.synth.getChannels()[this.nextChannel];

		// Convert 0-127 sentiment score into our mood instruments index
		final int miIndex = (int)Math.floor(
				Math.max(0,Math.min(127,sentimentScore))*this.moodInstruments.length/128);
		chan.programChange( this.moodInstruments[miIndex] );

		// Set the pan position
		chan.controlChange( this.PAN_CONTROLLER,
			this.getPanPosition( gl.longitude ) );

		// Set the volume based on the distance from the observer
		chan.controlChange( this.VOLUME_CONTROLLER,
			this.getUserDistanceVolume( gl, userInformation.location ) );

		// Set the brightness of the sound based on the sentiment score
		chan.controlChange( this.FILTER_RESONANCE_CONTROLLER, 40 );
		chan.controlChange( this.CUTOFF_FREQUENCY_CONTROLLER, (int)sentimentScore );

		// Stop the previous note on this channel
		chan.noteOff( this.notesOn[ this.nextChannel ] );

		// Play the note
		chan.noteOn(note.noteNumber, MIDIInstruments.FX_5_BRIGHTNESS);
		this.notesOn[this.nextChannel] = note.noteNumber;

		// Increment the channel to the next one.
		this.nextChannel++;
		this.nextChannel %= 16;

		// Check if it's a reserved channel
		while( this.reservedChannels.contains( this.nextChannel ) )
		{
			this.nextChannel++;
			this.nextChannel %= 16;
		}
	}

	/**
	 * 	Converts -180 to +180 longitude to a pan position
	 * 	0 - 127
	 *
	 *	@param longitude -180 to 180
	 *	@return pan position 0 to 127
	 */
	public int getPanPosition( final double longitude )
	{
		return (int)((longitude+180)*127/360);
	}

	/**
	 * 	Converts the user's distance into a volume value 0 - 127
	 *
	 *	@param comment The comment geo location
	 *	@param userLocation The user geo location
	 *	@return The volume
	 */
	public int getUserDistanceVolume( final GeoLocation comment, final GeoLocation userLocation )
	{
		// This distance calculation is ported from:
		// http://www.movable-type.co.uk/scripts/latlong.html
		final double R = 6371; // km
		final double dLat = Math.toRadians( userLocation.latitude-comment.latitude );
		final double dLon = Math.toRadians( userLocation.longitude-comment.longitude );
		final double lat1 = Math.toRadians( comment.latitude );
		final double lat2 = Math.toRadians( userLocation.latitude );

		final double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
		        Math.sin(dLon/2) * Math.sin(dLon/2) * Math.cos(lat1) * Math.cos(lat2);
		final double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
		final double d = R * c;

		// The maximum great circle distance is just over 20000km, so we'll
		// divide by that and multiply by the maximum our volume can be. In this
		// case to avoid completely inaudible updates, our minimum will be
		// 27, so range = 100
		final int volume = 127-((int)(d * 100 / 20020) + 27);

		return volume;
	}

	/**
	 *	{@inheritDoc}
	 * 	@see uk.ac.soton.ecs.wais.fest13.sound.SoundTranslator#stop()
	 */
	@Override
	public void stop()
	{
		if( this.sequencer != null )
			this.sequencer.stop();
		if( this.synth != null )
			this.synth.close();
	}

	/**
	 *	@param args
	 *	@throws MidiUnavailableException
	 * 	@throws InterruptedException
	 */
	public static void main( final String[] args ) throws MidiUnavailableException, InterruptedException
	{
		// Put our pretend user in London
		final UserInformation userInformation = new UserInformation();
		userInformation.location = new GeoLocation( 51.507222, -0.1275 );

		// Create 20 random social comments
		final MIDISoundTranslator mst = new MIDISoundTranslator();
		for( int i = 0; i < 36; i++ )
		{
			final SocialComment comment = new SocialComment();
			comment.location = new GeoLocation( /*Math.random()*9*/0, i*10-180 );

			mst.translate( Collections.singleton(comment), userInformation );
			Thread.sleep( 600 );
		}

		Thread.sleep( 50000 );
	}
}
