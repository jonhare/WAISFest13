/**
 * 
 */
package uk.ac.soton.ecs.wais.fest13.sound.midi;

import gnu.trove.list.array.TIntArrayList;

import java.util.Collection;
import java.util.Collections;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Synthesizer;

import org.openimaj.audio.util.WesternScaleNote;

import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoLocation;
import uk.ac.soton.ecs.wais.fest13.SocialComment;
import uk.ac.soton.ecs.wais.fest13.UserInformation;
import uk.ac.soton.ecs.wais.fest13.aggregators.AverageGeoLocation;
import uk.ac.soton.ecs.wais.fest13.sound.SoundTranslator;

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
	private Synthesizer synth;
	
	/** Next channel on which to play a note */
	private int nextChannel = 0;

	/** Notes to generate - based on the original Hang drum */
	private String[] notesToGenerate = new String[]{"D","A","A#","C","D","E","F"};
	
	/** Octaves of the notes to generate - based on the original Hang drum too */
	private int[] octavesOfNotesToGenerate = new int[]{3,3,3,4,4,4,4};

	/** The pan controller - to set where in the stereo field a note is played */
	public int PAN_CONTROLLER = 10;
	
	/** The volume controller - to audibly represent the distance from our user */
	public int VOLUME_CONTROLLER = 7;
	
	/** Channels not to use for general comment translation (0-indexed) */
	private TIntArrayList reservedChannels = new TIntArrayList( new int[]{9,1,2,3} );
	
	/** Aggregator to work out the average geo location */
	private AverageGeoLocation avGeoLocAggregator = new AverageGeoLocation();
	
	/** 
	 * 	A memory of which notes are on which channels (to turn them off before
	 *  we change the sound or change the channel setup.
	 */
	private int[] notesOn = new int[16];
	
	/**
	 * 	Default constructor
	 *	@throws MidiUnavailableException If a synth could not be created
	 */
	public MIDISoundTranslator() throws MidiUnavailableException
	{
		this.synth = MidiSystem.getSynthesizer();
		synth.open();
	}

	/**
	 *	{@inheritDoc}
	 * 	@see uk.ac.soton.ecs.wais.fest13.sound.SoundTranslator#translate(java.util.Collection, uk.ac.soton.ecs.wais.fest13.UserInformation)
	 */
	@Override
	public void translate( Collection<SocialComment> comment, UserInformation userInformation )
	{
		if(comment.size() == 0) return;
		// The average geo location of all the comments
		GeoLocation gl = avGeoLocAggregator.aggregate( comment, userInformation );
		
		// Give a bit more range to the notes we've provided
		int alterOctave = (int)(Math.random()*3)-1;
		
		// Create the note
		int indx = (int)(Math.random() * notesToGenerate.length);
		WesternScaleNote note = WesternScaleNote.createNote( 
				notesToGenerate[indx], 
				octavesOfNotesToGenerate[indx]+alterOctave );
		
		// Get the channel
//		System.out.println( nextChannel );
		MidiChannel chan = synth.getChannels()[nextChannel];
		
		// For now we will just use the piano
		chan.programChange( 0 );
		
		// Set the pan position
		chan.controlChange( PAN_CONTROLLER, 
			getPanPosition( gl.longitude ) );
		
		// Set the volume based on the distance from the observer
		chan.controlChange( VOLUME_CONTROLLER, 
			getUserDistanceVolume( gl, userInformation.location ) );
		
		// Stop the previous note on this channel
		chan.noteOff( notesOn[ nextChannel ] );
		
		// Play the note
		chan.noteOn( note.noteNumber, 100 );
		notesOn[ nextChannel ] = note.noteNumber;
		
		// Increment the channel to the next one.
		nextChannel++;
		nextChannel %= 16;
		
		// Check if it's a reserved channel
		while( reservedChannels.contains( nextChannel ) )
		{
			nextChannel++;
			nextChannel %= 16;
		}
	}
	
	/**
	 * 	Converts -180 to +180 longitude to a pan position
	 * 	0 - 127
	 * 
	 *	@param longitude -180 to 180
	 *	@return pan position 0 to 127
	 */
	public int getPanPosition( double longitude )
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
	public int getUserDistanceVolume( GeoLocation comment, GeoLocation userLocation )
	{
		// This distance calculation is ported from:
		// http://www.movable-type.co.uk/scripts/latlong.html
		double R = 6371; // km
		double dLat = Math.toRadians( userLocation.latitude-comment.latitude );
		double dLon = Math.toRadians( userLocation.longitude-comment.longitude );
		double lat1 = Math.toRadians( comment.latitude );
		double lat2 = Math.toRadians( userLocation.latitude );

		double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
		        Math.sin(dLon/2) * Math.sin(dLon/2) * Math.cos(lat1) * Math.cos(lat2); 
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a)); 
		double d = R * c;
		
		// The maximum great circle distance is just over 20000km, so we'll
		// divide by that and multiply by the maximum our volume can be. In this
		// case to avoid completely inaudible updates, our minimum will be
		// 27, so range = 100
		int volume = (int)(d * 100 / 20020) + 27;
		
		return volume;
	}

	/**
	 *	{@inheritDoc}
	 * 	@see uk.ac.soton.ecs.wais.fest13.sound.SoundTranslator#stop()
	 */
	@Override
	public void stop()
	{
		this.synth.close();
	}

	/**
	 *	@param args
	 *	@throws MidiUnavailableException
	 * 	@throws InterruptedException 
	 */
	public static void main( String[] args ) throws MidiUnavailableException, InterruptedException
	{		
		// Put our pretend user in London
		UserInformation userInformation = new UserInformation();
		userInformation.location = new GeoLocation( 51.507222, -0.1275 );

		// Create 20 random social comments
		MIDISoundTranslator mst = new MIDISoundTranslator();
		for( int i = 0; i < 20; i++ )
		{
			SocialComment comment = new SocialComment();
			comment.location = new GeoLocation( Math.random()*90, Math.random()*360-180 );
			
			mst.translate( Collections.singleton(comment), userInformation );
			Thread.sleep( 600 );
		}
		
		Thread.sleep( 5000 );
	}
}
