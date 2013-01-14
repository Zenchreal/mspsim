package se.sics.mspsim.platform.uw;

import java.io.IOException;

import se.sics.mspsim.config.MSP430fr5728Config;
import se.sics.mspsim.core.MSP430Constants;
import se.sics.mspsim.platform.GenericNode;
import se.sics.mspsim.util.ArgumentManager;

public class AnalysisNode extends GenericNode {

	public AnalysisNode() {
		super("Analysis", new MSP430fr5728Config());
	}

	@Override
	public void setupNode() {
		// Connect to any IO pins here.
		
		if (!config.getPropertyAsBoolean("nogui", true)) {
			// Open any debug windows here.
		}
		
		cpu.writeRegister(MSP430Constants.SP, 0x2000);
	}

	@Override
	public int getModeMax() {
		return 0;
	}
	
	public static void main(String[] args) throws IOException {
		AnalysisNode node = new AnalysisNode();
        ArgumentManager config = new ArgumentManager();
        config.handleArguments(args);
        node.setupArgs(config);
	}

}
