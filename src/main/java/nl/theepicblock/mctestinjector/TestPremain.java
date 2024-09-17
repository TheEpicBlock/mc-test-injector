package nl.theepicblock.mctestinjector;

import nilloader.api.ClassTransformer;
import nilloader.api.NilLogger;

// All entrypoint classes must implement Runnable.
public class TestPremain implements Runnable {

	// NilLoader comes with a logger abstraction that Does The Right Thing depending on the environment.
	// You should always use it.
	public static final NilLogger log = NilLogger.get("mc-test-injector");
	
	@Override
	public void run() {
		// Any class transformers need to be registered with NilLoader like this.
		ClassTransformer.register(new TestInjectionTransformer());
	}

}
