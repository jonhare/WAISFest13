package uk.ac.soton.ecs.wais.fest13;

import org.openimaj.util.function.Operation;
import org.openimaj.util.function.Predicate;

public class PassThrough<T> implements Predicate<T> {

	private Operation<T> op;

	public PassThrough(Operation<T> op) {
		this.op = op;
	}

	@Override
	public boolean test(T object) {
		if(op != null)
			op.perform(object);
		return true;
	}

}
