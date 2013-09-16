package uk.ac.soton.ecs.wais.fest13.sound.midi;

import gnu.trove.list.array.TIntArrayList;

import java.util.Iterator;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import org.openimaj.audio.util.WesternScaleNote;

/**
 * A class that provides some form of background music.
 *
 * @author David Dupplaw (dpd@ecs.soton.ac.uk)
 * @created 12 Sep 2013
 * @version $Author$, $Revision$, $Date$
 */
public class BackgroundMusic implements Iterable<Sequence>, Iterator<Sequence>
{
	/**
	 * Get a program change message.
	 *
	 * @param chan The channel
	 * @param prog The new programm
	 * @param tick The timestamp
	 * @return A program change message
	 * @throws InvalidMidiDataException
	 */
	private MidiEvent getProgChange( final int chan, final int prog, final long tick ) throws InvalidMidiDataException
	{
		final ShortMessage mm = new ShortMessage();
		mm.setMessage( 0xC0 + chan, prog, 0x00 );
		return new MidiEvent( mm, tick );
	}

	/**
	 * Get a meta message that's the end-of-track message.
	 *
	 * @param tick The timestamp
	 * @return The end-of-track message
	 * @throws InvalidMidiDataException
	 */
	private MidiEvent getEnd( final long tick ) throws InvalidMidiDataException
	{
		final MetaMessage mt = new MetaMessage();
		final byte[] bet =
		{}; // empty array
		mt.setMessage( 0x2F, bet, 0 );
		return new MidiEvent( mt, tick );
	}

	/**
	 * Get a note-on message
	 *
	 * @param chan The channel
	 * @param noteNumber The note number
	 * @param vel The velocity
	 * @param tick The timestamp
	 * @return The new note-on message
	 * @throws InvalidMidiDataException
	 */
	private MidiEvent noteOn( final int chan, final int noteNumber, final int vel, final long tick ) throws InvalidMidiDataException
	{
		final ShortMessage mm = new ShortMessage();
		mm.setMessage( 0x90 + chan, noteNumber, vel );
		return new MidiEvent( mm, tick );
	}

	/**
	 * Get a note-off message
	 *
	 * @param chan The channel
	 * @param noteNumber The note number
	 * @param tick The timestamp
	 * @return A note-off message
	 * @throws InvalidMidiDataException
	 */
	private MidiEvent noteOff( final int chan, final int noteNumber, final long tick ) throws InvalidMidiDataException
	{
		final ShortMessage mm = new ShortMessage();
		mm.setMessage( 0x80 + chan, noteNumber, 0x40 );
		return new MidiEvent( mm, tick );
	}

	/**
	 * This is the background music encoded into an ad-hoc ASCII representation.
	 * Each pulse (a pulse is defined by the number of ticks in #pptTracks) is
	 * separated by whitespace. A "." has no effect. A "/" is end of track. A
	 * "-" is note off. Other commands within a pulse are separated by "+". "H"
	 * sets the MIDI channel (1-based). "P" sets the program change (1-based).
	 * Other commands are note strings (i.e. note and octave number),
	 * potentially followed by a "." then a velocity value. Velocities, and
	 * channels are latched so continue until a change is found. A note off is
	 * provided before each note-on for any notes that are on. If you are
	 * playing a chord, you can force notes off for specific notes by preceding
	 * a note command with a "-". If it's within a pulse command, you still need
	 * the + (i.e. D2.40+-C2) Chords are all played at the same timestamp,
	 * whereas other commands within the same pulse will be separated by a
	 * single tick. The parser for all this is in the {@link #getSequence()}
	 * method. It's a little verbose, but it works and is easier than creating
	 * all this stuff programmatically (just about)!
	 */
	private final String[] tracks = new String[]
	{ 		"H2+P34+D2.60       D2 D3 - . D2 . . D2 - . . . . D3 D3 /",
			"H3+P44+D2.15+E4.20 . . . . . . . . . . . F4 . . . /",
			"H10+F#3.40 F#3.20 F#3 F#3 F#3.40 F#3.20 F#3 F#3 " +
					"F#3.40 F#3.20 F#3 F#3 F#3.40 F#3.20 F#3 F#3 /",
			"H10 . . . . . . . . . . . E2.40 . . . /",
			"H1+D4.40+A#4+C5 . . . . . . . . . . -D4+-A#4+D4+A4+D5 . . . / ",
			"H1+D4.40+A4+F5 . . . . . . . . . . -D4+-A4+E4+C#5+E5 . . . / ",
			"H3+D2.30+F4.35 . . . . . . . . . . . . . . . /",
			"H3+P44+D5.30+A5+D6 -D5+-A5+-D6 . D5+A5+D6 -D5+-A5+-D6 . . . . . . . D5+A6+D6 -D5+-A6+-D6 . . / ",
			"H10+B2.60 . . . . . . . B2.60 . . . . . . . / "
	};

	/** States which tracks (above) are transposable */
	private final boolean[] transposable = new boolean[]{
		true, true, false, false, true, true, true, true, false
	};

	private int currentMood = 0;

	/**
	 * Lists the tracks that should be unmuted for each mood level. Tracks are
	 * indexed into the #tracks member, so 0-based.
	 */
	private final TIntArrayList[] moods = new TIntArrayList[] {
			new TIntArrayList( new int[] { 1 } ),
			new TIntArrayList( new int[] { 0, 1 } ),
			new TIntArrayList( new int[] { 0, 1, 3 } ),
			new TIntArrayList( new int[] { 0, 1, 2, 3 } ),
			new TIntArrayList( new int[] { 0, 1, 2, 3, 4, 8 } ),
			new TIntArrayList( new int[] { 0, 6, 2, 3, 5, 8 } ),
			new TIntArrayList( new int[] { 0, 6, 2, 3, 5, 8 } )
	};

	/** States which moods should be transposed */
	private final boolean transposeOnMood[] = new boolean[] {
		false, false, false, false, true, true, true
	};

	/** Some semi-tone transposes to do */
	private final int transposes[] = new int[]{
			0, 0, 0, -7, 7
	};

	/** Notes based on the original hang-drum */
	private final String[] LOW_MOOD_NOTES = new String[]{"D3","A3","A#3","C4","D4","E4","F4"};

	/** High-mood notes */
//	private final String[] HIGH_MOOD_NOTES = new String[]{"D3","F3","G3","A#3","C#4","D4","E4"};

	/** Notes to play that'll fit with the background */
	private final String[][] fittingNotes = new String[][]
	{
			this.LOW_MOOD_NOTES, this.LOW_MOOD_NOTES, this.LOW_MOOD_NOTES, this.LOW_MOOD_NOTES, this.LOW_MOOD_NOTES,
			this.LOW_MOOD_NOTES, this.LOW_MOOD_NOTES,
	};

	/** Number of ticks in each pulse of the tracks definition */
	private final int pptTracks = 6;

	/**
	 * May return null if the sequence was unable to be created.
	 *
	 * @return The sequence
	 */
	public Sequence getSequence()
	{
		return this.getSequence( 0 );
	}

	/**
	 * May return null if the sequence was unable to be created.
	 * @return The sequence
	 */
	public Sequence getSequence( final int transpose )
	{
		// Create a new sequence
		Sequence s;
		try
		{
			s = new Sequence( javax.sound.midi.Sequence.PPQ, 24 );
		}
		catch( final InvalidMidiDataException e1 )
		{
			e1.printStackTrace();
			return null;
		}

		final double velocityScalar = 1;

		// Loop through each of the tracks.
		for( int t = 0; t < this.tracks.length; t++ )
		{
			final String track = this.tracks[t];

			// Create a sequencer track for the part
			final Track seqTrack = s.createTrack();

			// Split into pulses
			final String[] pulses = track.split( "\\s+" );

			// Our cache variables
			long timestampTick = 0;
			int currentChannel = 0;
			int currentVelocity = 100;
			int currentNoteOn = -1;

			final int tp = (this.transposable[t] ? transpose : 0);

			// Loop through the pulses
			for( final String pulse : pulses )
			{
				// Split the pulse into commands
				final String[] commands = pulse.split( "\\+" );
				int a = this.pptTracks;

				// Loop through each of the commands
				boolean samePulse = false;
				for( final String command : commands )
				{
					try
					{
						switch (command.charAt( 0 ))
						{
						// Channel definition.
							case 'H':
								currentChannel = Integer.parseInt( command.substring( 1 ) ) - 1;
								break;
							// Program Change
							case 'P':
								final int prog = Integer.parseInt( command.substring( 1 ) ) - 1;
								seqTrack.add( this.getProgChange( currentChannel, prog, timestampTick ) );

								a--;
								timestampTick++;
								break;
							// Note off
							case '-':
								if( command.length() == 1 )
									seqTrack.add( this.noteOff( currentChannel, currentNoteOn, timestampTick ) );
								else
								{
									final WesternScaleNote note = WesternScaleNote.createNote( command.substring( 1 ) );
									seqTrack.add( this.noteOff( currentChannel, note.noteNumber+tp, timestampTick ) );
								}

								a--;
								timestampTick++;
								break;
							// End of track
							case '/':
								seqTrack.add( this.getEnd( timestampTick ) );
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
									currentVelocity = Integer.parseInt(
											command.substring( command.indexOf( '.' ) + 1 ) );
								}
								// Otherwise it's just a note message.
								else
								{
									noteString = command;
								}

								// Create the note.
								final WesternScaleNote note = WesternScaleNote.createNote( noteString );

								// If we're in a new pulse and there's already a
								// note on, we'll turn off the last note.
								if( currentNoteOn > 0 && !samePulse )
									seqTrack.add( this.noteOff( currentChannel, currentNoteOn, timestampTick ) );

								seqTrack.add( this.noteOn( currentChannel,
										note.noteNumber+tp, (int) (currentVelocity * velocityScalar), timestampTick ) );
								currentNoteOn = note.noteNumber+tp;

								if( samePulse )
								{
									a--;
									timestampTick++;
								}

								samePulse = true;
						}
					}
					catch( final NumberFormatException e )
					{
						e.printStackTrace();
					}
					catch( final InvalidMidiDataException e )
					{
						e.printStackTrace();
					}
				}

				// Go on to the next pulse
				timestampTick += a;
			}
		}

		// WesternScaleNote d2 = WesternScaleNote.createNote( "D2" );
		// WesternScaleNote e6 = WesternScaleNote.createNote( "E6" );
		//
		// final int BASS_DRUM_2 = 35;
		//
		// final int PICK_BASS_PROG = 33;
		// final int TREMELO_STRINGS_PROG = 43;
		//
		// try
		// {
		// s = new Sequence( javax.sound.midi.Sequence.PPQ, 24 );
		//
		// Track bassTrack = s.createTrack();
		// Track stringsTrack = s.createTrack();
		// Track bassDrumTrack = s.createTrack();
		// Track hihatTrack = s.createTrack();
		//
		// // Channel 0, Program 0, at tick 0
		// bassTrack.add( getProgChange( 1, PICK_BASS_PROG, 0 ) );
		// stringsTrack.add( getProgChange( 2, TREMELO_STRINGS_PROG, 0 ) );
		//
		// bassTrack.add( noteOn( 1, d2.noteNumber, 40, 1 ) );
		// stringsTrack.add( noteOn( 2, d2.noteNumber, 25, 1 ) );
		// stringsTrack.add( noteOn( 2, e6.noteNumber, 35, 1 ) );
		//
		// bassTrack.add( noteOff( 1, d2.noteNumber, 8 ) );
		//
		// bassDrumTrack.add( noteOn( 9, BASS_DRUM_2, 40, 1 ) );
		//
		// bassTrack.add( getEnd( 24 ) );
		// stringsTrack.add( getEnd( 24 ) );
		// bassDrumTrack.add( getEnd( 24 ) );
		// hihatTrack.add( getEnd( 24 ) );
		// }
		// catch( InvalidMidiDataException e )
		// {
		// e.printStackTrace();
		// }

		return s;
	}

	/**
	 * Returns the number of ticks in the first track of the background music.
	 * TODO: Maybe this ought to scan for the longest track?
	 *
	 * @return The length in ticks @ 24PPQ
	 */
	public long getLength()
	{
		return (this.tracks[0].split( "\\s+" ).length - 1) * this.pptTracks;
	}

	/**
	 * Returns the number of moods available.
	 *
	 * @return The number of moods.
	 */
	public int getNumberOfMoods()
	{
		return this.moods.length;
	}

	/**
	 * Sets the sequencer to the appropriate mood
	 *
	 * @param seq
	 */
	public void setMood( int moodIndex, final Sequencer seq )
	{
		if( moodIndex < 0 ) moodIndex = 0;
		if( moodIndex >= this.moods.length ) moodIndex = this.moods.length-1;
		for( int i = 0; i < this.tracks.length; i++ )
			seq.setTrackMute( i, !this.moods[moodIndex].contains( i ) );
		this.currentMood = moodIndex;
	}

	/**
	 * Get the current mood index.
	 *
	 * @return the current mood index.
	 */
	public int getCurrentMood()
	{
		return this.currentMood;
	}

	/**
	 * 	Get notes that will fit the mood index
	 *	@param moodIndex The mood index
	 *	@return
	 */
	public String[] getNotesToFitMood( final int moodIndex )
	{
		return this.fittingNotes[moodIndex];
	}

	/**
	 *	{@inheritDoc}
	 * 	@see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<Sequence> iterator()
	{
		return this;
	}

	/**
	 *	{@inheritDoc}
	 * 	@see java.util.Iterator#hasNext()
	 */
	@Override
	public boolean hasNext()
	{
		return true;
	}

	/**
	 *	{@inheritDoc}
	 * 	@see java.util.Iterator#next()
	 */
	@Override
	public Sequence next()
	{
		if( this.transposeOnMood[this.currentMood] )
		{
			final int i = (int)(Math.random() * this.transposes.length);
			return this.getSequence( this.transposes[i] );
		}

		return this.getSequence();
	}

	/**
	 *	{@inheritDoc}
	 * 	@see java.util.Iterator#remove()
	 */
	@Override
	public void remove()
	{
		// Not implemented
	}
}