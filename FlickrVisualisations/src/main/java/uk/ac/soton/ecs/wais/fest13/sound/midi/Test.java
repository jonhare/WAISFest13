package uk.ac.soton.ecs.wais.fest13.sound.midi;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Synthesizer;

import org.openimaj.audio.util.WesternScaleNote;

public class Test {
	public static void main(String[] args) throws MidiUnavailableException, InterruptedException {
		final Synthesizer synth = MidiSystem.getSynthesizer();
		synth.open();

		final WesternScaleNote[] notes = {
				WesternScaleNote.createNote("G", 4),
				WesternScaleNote.createNote("A", 4),
				WesternScaleNote.createNote("F", 4),
				WesternScaleNote.createNote("F", 3),
				WesternScaleNote.createNote("C", 4),
		};

		final MidiChannel chan = synth.getChannels()[0];

		// For now we will just use the piano
		chan.programChange(15);

		int incr = 1;
		for (int i = 1; i >= 1; i += incr) {
			for (final WesternScaleNote note : notes) {
				chan.noteOn(note.noteNumber, 100);
				Thread.sleep(1000 / i);
				chan.noteOff(note.noteNumber);
			}
			if (i == 10)
				incr *= -1;
			System.out.println(i);
		}
		chan.allNotesOff();
		synth.close();
	}
}
