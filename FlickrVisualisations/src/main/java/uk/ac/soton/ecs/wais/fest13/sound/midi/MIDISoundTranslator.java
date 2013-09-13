/**
 * 
 */
package uk.ac.soton.ecs.wais.fest13.sound.midi;

import gnu.trove.list.array.TIntArrayList;

import java.util.Collection;
import java.util.Collections;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Track;

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
	/**
	 * 	A class that provides some form of background music. 
	 *
	 *	@author David Dupplaw (dpd@ecs.soton.ac.uk)
	 *  @created 12 Sep 2013
	 *	@version $Author$, $Revision$, $Date$
	 */
	protected class BackgroundMusic
	{
		/**
		 * 	Get a program change message.
		 *	@param chan The channel
		 *	@param prog The new programm
		 *	@param tick The timestamp
		 *	@return A program change message
		 *	@throws InvalidMidiDataException
		 */
		private MidiEvent getProgChange( int chan, int prog, long tick ) 
				throws InvalidMidiDataException
		{
			ShortMessage mm = new ShortMessage();
			mm.setMessage( 0xC0 + chan, prog, 0x00 );
			return new MidiEvent( mm, tick );		
		}
		
		/**
		 * 	Get a meta message that's the end-of-track message.
		 *	@param tick The timestamp
		 *	@return The end-of-track message
		 *	@throws InvalidMidiDataException
		 */
		private MidiEvent getEnd( long tick ) throws InvalidMidiDataException
		{
			MetaMessage mt = new MetaMessage();
	        byte[] bet = {}; // empty array
			mt.setMessage( 0x2F, bet, 0 );
			return new MidiEvent( mt, tick );			
		}

		/**
		 * 	Get a note-on message
		 *	@param chan The channel
		 *	@param noteNumber The note number
		 *	@param vel The velocity
		 *	@param tick The timestamp
		 *	@return The new note-on message
		 *	@throws InvalidMidiDataException
		 */
		private MidiEvent noteOn( int chan, int noteNumber, int vel, long tick ) 
				throws InvalidMidiDataException
		{
			ShortMessage mm = new ShortMessage();
			mm.setMessage( 0x90+chan, noteNumber, vel );
			return new MidiEvent( mm, tick );
		}

		/**
		 * 	Get a note-off message
		 *	@param chan The channel
		 *	@param noteNumber The note number
		 *	@param tick The timestamp
		 *	@return A note-off message
		 *	@throws InvalidMidiDataException
		 */
		private MidiEvent noteOff( int chan, int noteNumber, long tick ) 
				throws InvalidMidiDataException
		{
			ShortMessage mm = new ShortMessage();
			mm.setMessage(0x80+chan,noteNumber,0x40);
			return new MidiEvent( mm, tick );
		}
		
		/** 
		 * This is the background music encoded into an ad-hoc ASCII
		 * representation. Each pulse (a pulse is defined by the number of
		 * ticks in #pptTracks) is separated by whitespace. A "." has no effect.
		 * A "/" is end of track. A "-" is note off. Other commands within
		 * a pulse are separated by "+". "H" sets the MIDI channel (1-based). 
		 * "P" sets the program change (1-based). Other commands are note strings
		 * (i.e. note and octave number), potentially followed by a "." then a
		 * velocity value. Velocities, and channels are latched so continue
		 * until a change is found. A note off is provided before each note-on
		 * for any notes that are on. If you are playing a chord, you can force
		 * notes off for specific notes by preceding a note command with a "-".
		 * If it's within a pulse command, you still need the + (i.e. D2.40+-C2)
		 * The parser for all this is in the {@link #getSequence()} method. 
		 * It's a little verbose, but it works and is easier than creating all
		 * this stuff programmatically (just about)!
		 */
		private final String[] tracks = new String[] {
				"H2+P34+D2.40       - . . . . . . D2 - . . . . . . /",
				"H3+P44+D2.30+E6.35 . . . . . . . F6 . . . . . . . /",
				"H10+F#3.60 F#3.20 F#3 F#3 F#3.60 F#3.20 F#3 F#3 "+
						"F#3.60 F#3.20 F#3 F#3 F#3.60 F#3.20 F#3 F#3 /",
				"H10+C3.40 . . . C3 . . . C3 . . . C3 . . . /"
		};

		/** Number of ticks in each pulse of the tracks definition */
		private int pptTracks = 6; 
		
		/**
		 * 	May return null if the sequence was unable to be created.
		 *	@return The sequence
		 */
		public Sequence getSequence()
		{
			// Create a new sequence
			Sequence s;
			try
			{
				s = new Sequence( javax.sound.midi.Sequence.PPQ, 24 );
			}
			catch( InvalidMidiDataException e1 )
			{
				e1.printStackTrace();
				return null;
			}

			// Loop through each of the tracks.
			for( String track: tracks )
			{
				// Create a sequencer track for the part
				Track seqTrack = s.createTrack();
				
				// Split into pulses
				String[] pulses = track.split( "\\s+" );
				
				// Our cache variables
				long timestampTick = 0;
				int currentChannel = 0;
				int currentVelocity = 100;
				int currentNoteOn = -1;
				
				// Loop through the pulses
				for( String pulse : pulses )
				{
					// Split the pulse into commands
					String[] commands = pulse.split( "\\+" );
					
					// Loop through each of the commands
					boolean samePulse = false;
					for( String command : commands )
					{
						System.out.println( timestampTick+" @ Command: "+command);
						try
						{
							switch( command.charAt( 0 ) )
							{
								// Channel definition.
								case 'H': 
									currentChannel = Integer.parseInt( command.substring(1) )-1;
									break;
								// Program Change
								case 'P':
									int prog = Integer.parseInt( command.substring( 1 ) )-1;
									seqTrack.add( getProgChange( currentChannel, prog, timestampTick ) );
									break;
								// Note off
								case '-':
									if( command.length() == 1 )
										seqTrack.add( noteOff( currentChannel, currentNoteOn, timestampTick ) );
									else
									{
										WesternScaleNote note = WesternScaleNote.createNote( command.substring(1) );
										seqTrack.add( noteOff( currentChannel, note.noteNumber, timestampTick ) );
									}
									break;
								// End of track
								case '/':
									seqTrack.add( getEnd( timestampTick ) );
									break;
								// A pulse with no commands
								case '.':
									break;
								// Note on
								default:
									String noteString = null;
									
									// If there's a . in the string, it contains
									// a velocity definition. So parse it out.
									if( command.contains( "." ) )
									{
										noteString = command.substring( 0, command.indexOf( '.' ) );
										currentVelocity = Integer.parseInt( command.substring( command.indexOf('.')+1 ) );
									}
									// Otherwise it's just a note message.
									else
									{
										noteString = command;
									}
									
									// Create the note.
									WesternScaleNote note = WesternScaleNote.createNote( noteString );
									
									// If we're in a new pulse and there's already a
									// note on, we'll turn off the last note.
									if( currentNoteOn > 0 && !samePulse )
										seqTrack.add( noteOff( currentChannel, currentNoteOn, timestampTick ) );
									
									seqTrack.add( noteOn( currentChannel, note.noteNumber, currentVelocity, timestampTick ) );
									currentNoteOn = note.noteNumber;
									samePulse = true;
							}
						}
						catch( NumberFormatException e )
						{
							e.printStackTrace();
						}
						catch( InvalidMidiDataException e )
						{
							e.printStackTrace();
						}
					}

					// Go on to the next pulse
					timestampTick += pptTracks;
				}
			}
			
//			WesternScaleNote d2 = WesternScaleNote.createNote( "D2" );
//			WesternScaleNote e6 = WesternScaleNote.createNote( "E6" );
//			
//			final int BASS_DRUM_2 = 35;
//			
//			final int PICK_BASS_PROG = 33;
//			final int TREMELO_STRINGS_PROG = 43;
//			
//			try
//			{
//				s = new Sequence( javax.sound.midi.Sequence.PPQ, 24 );
//				
//				Track bassTrack = s.createTrack();
//				Track stringsTrack = s.createTrack();
//				Track bassDrumTrack = s.createTrack();
//				Track hihatTrack = s.createTrack();
//				
//				// Channel 0, Program 0, at tick 0
//				bassTrack.add( getProgChange( 1, PICK_BASS_PROG, 0 ) );
//				stringsTrack.add( getProgChange( 2, TREMELO_STRINGS_PROG, 0 ) );
//
//				bassTrack.add( noteOn( 1, d2.noteNumber, 40, 1 ) );
//				stringsTrack.add( noteOn( 2, d2.noteNumber, 25, 1 ) );
//				stringsTrack.add( noteOn( 2, e6.noteNumber, 35, 1 ) );
//
//				bassTrack.add( noteOff( 1, d2.noteNumber, 8 ) );
//				
//				bassDrumTrack.add( noteOn( 9, BASS_DRUM_2, 40, 1 ) );
//
//				bassTrack.add( getEnd( 24 ) );
//				stringsTrack.add( getEnd( 24 ) );
//				bassDrumTrack.add( getEnd( 24 ) );
//				hihatTrack.add( getEnd( 24 ) );
//			}
//			catch( InvalidMidiDataException e )
//			{
//				e.printStackTrace();
//			}
			
			return s;
		}
		
		/**
		 * 	Returns the number of ticks in the first track of the
		 * 	background music.  TODO: Maybe this ought to scan for the longest track?
		 *	@return The length in ticks @ 24PPQ
		 */
		public long getLength()
		{
			return (tracks[0].split( "\\s+" ).length-1) * pptTracks;
		}
	}
	
	/** The MIDI synth in use */
	private Synthesizer synth;
	
	/** Next channel on which to play a note */
	private int nextChannel = 0;

	/** Notes to generate - based on the original Hang drum */
	private String[] notesToGenerate = new String[]{"D3","A3","A#3","C4","D4","E4","F4"};
	
	/** The pan controller - to set where in the stereo field a note is played */
	public int PAN_CONTROLLER = 10;
	
	/** The volume controller - to audibly represent the distance from our user */
	public int VOLUME_CONTROLLER = 7;
	
	/** Channels not to use for general comment translation (0-indexed) */
	private TIntArrayList reservedChannels = new TIntArrayList( new int[]{9,1,2,3} );
	
	/** Aggregator to work out the average geo location */
	private AverageGeoLocation avGeoLocAggregator = new AverageGeoLocation();
	
	/** Whether to generate background tracks */
	private boolean useBackground = true;
	
	/** 
	 * 	A memory of which notes are on which channels (to turn them off before
	 *  we change the sound or change the channel setup.
	 */
	private int[] notesOn = new int[16];

	/** The sequencer to use to run the background music */
	private Sequencer sequencer;

	private BackgroundMusic backgroundMusic;
	
	/**
	 * 	Default constructor
	 *	@throws MidiUnavailableException If a synth could not be created
	 */
	public MIDISoundTranslator() throws MidiUnavailableException
	{
		// Open the synthesizer to play the music
		this.synth = MidiSystem.getSynthesizer();
		synth.open();
		
		// Setup the sequencer for the background music, if we're going
		// to use it.
		if( useBackground )
		{
			this.sequencer = MidiSystem.getSequencer();
			if( sequencer != null )
			{
				sequencer.open();

				try
				{
					this.backgroundMusic = new BackgroundMusic();
					sequencer.setSequence( backgroundMusic.getSequence() );
					sequencer.setLoopEndPoint( 0 );
					sequencer.setLoopEndPoint( backgroundMusic.getLength() );
					sequencer.setLoopCount( Sequencer.LOOP_CONTINUOUSLY );
					sequencer.start();
					sequencer.setTempoInBPM( 60 );
				}
				catch( InvalidMidiDataException e )
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
	public void translate( Collection<SocialComment> comment, UserInformation userInformation )
	{
		// The average geo location of all the comments
		GeoLocation gl = avGeoLocAggregator.aggregate( comment, userInformation );
		
		// Give a bit more range to the notes we've provided
		int alterOctave = (int)(Math.random()*3)-1;
		
		// Create the note
		int indx = (int)(Math.random() * notesToGenerate.length);
		WesternScaleNote note = WesternScaleNote.createNote( 
				notesToGenerate[indx] );
		note.noteNumber += 12 * alterOctave;
		
		// Get the channel
		System.out.println( nextChannel );
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
