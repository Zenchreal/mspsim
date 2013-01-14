package se.sics.mspsim.config;

import java.util.ArrayList;

import se.sics.mspsim.core.DMA;
import se.sics.mspsim.core.IOUnit;
import se.sics.mspsim.core.InterruptMultiplexer;
import se.sics.mspsim.core.MSP430Config;
import se.sics.mspsim.core.MSP430Core;
import se.sics.mspsim.core.Timer;

public class MSP430fr5728Config extends MSP430Config {

	public MSP430fr5728Config() {
		MSP430XArch = true;
		watchdogOffset = 0x15C;
		maxInterruptVector = 63;
		maxMemIO = 0x1000;
		
		TimerConfig timerA = new TimerConfig(53, 52, 3, 0x340, Timer.TIMER_Ax149, "TimerA", 0x36E);
 
        timerConfig = new TimerConfig[] {timerA};
        
        infoMemConfig(0x1800, 128 * 2);
        mainFlashConfig(0xC200, 0x3E00);
        ramConfig(0x1C00, 0x0400);
	}

	@Override
	public int setup(MSP430Core cpu, ArrayList<IOUnit> ioUnits) {
		DMA dma = new DMA("dma", cpu, cpu.memory, 0);
		
		// This used to be hardcoded in DMA
		dma.DMACTL0 = 0x500;
		dma.DMACTL1 = 0x502;
		cpu.setIORange(dma.DMACTL0, 1, dma);
        cpu.setIORange(dma.DMACTL1, 1, dma);
        
        // Channel configurations
        dma.DMAxCTL = 0x510;
        dma.DMAxSA = 0x512;
        dma.DMAxDA = 0x516;
        dma.DMAxSZ = 0x51A;
        cpu.setIORange(0x510, 24, dma);

        /* configure the DMA */
        dma.setInterruptMultiplexer(new InterruptMultiplexer(cpu, 0));

        ioUnits.add(dma);
		return 1;
	}

}
