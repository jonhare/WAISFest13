package uk.ac.soton.ecs.wais.fest13.demos;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.openimaj.logger.LoggerUtils;
import org.openimaj.util.data.Context;
import org.openimaj.util.function.Function;
import org.openimaj.util.function.Operation;
import org.openimaj.util.stream.CollectionStream;
import org.openimaj.util.stream.Stream;

public class CachedTransform<T> implements Function<Stream<T>, Stream<T>> {
	public CachedTransform() {
		LoggerUtils.prepareConsoleLogger();
	}
	private final static Logger logger = Logger.getLogger(CachedTransform.class);
	@Override
	public Stream<T> apply(Stream<T> in) {
		final ArrayList<T> all = new ArrayList<T>();
		in.forEach(new Operation<T>() {
			int read = 0;

			@Override
			public void perform(T object) {
				all.add(object);
				read++;
				if(read%10000 == 0){
					System.out.println("Read: " + read);
				}
			}
			
		});
		
		return new CollectionStream<T>(all);
	}

}
