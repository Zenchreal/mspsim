/**
 * Copyright (c) 2007, Swedish Institute of Computer Science.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the Institute nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE INSTITUTE AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE INSTITUTE OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 * This file is part of MSPSim.
 *
 * $Id$
 *
 * -----------------------------------------------------------------
 *
 * ESBGui
 *
 * Author  : Joakim Eriksson
 * Created : Sun Oct 21 22:00:00 2007
 * Updated : $Date$
 *           $Revision$
 */

package se.sics.mspsim.platform.esb;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;
import javax.swing.ImageIcon;

import se.sics.mspsim.chip.Beeper;
import se.sics.mspsim.core.ADC12;
import se.sics.mspsim.core.ADCInput;
import se.sics.mspsim.core.IOUnit;
import se.sics.mspsim.core.MSP430;
import se.sics.mspsim.core.USART;
import se.sics.mspsim.platform.AbstractNodeGUI;
import se.sics.mspsim.ui.SerialMon;

public class ESBGui extends AbstractNodeGUI implements ADCInput {

  private static final long serialVersionUID = -139331418649524704L;

  public static final int GREEN_X = 2;
  public static final int YELLOW_X = 9;
  public static final int RED_X = 16;
  public static final int LED_Y = 4;
  private static final Rectangle LED_BOUNDS = new Rectangle(GREEN_X - 2, LED_Y - 4, RED_X + 6, LED_Y + 10);

  public static final Color RED_TRANS = new Color(0xff,0x40,0x40,0xa0);
  public static final Color YELLOW_TRANS = new Color(0xff, 0xff, 0x00, 0xa0);
  public static final Color GREEN_TRANS = new Color(0x40, 0xf0, 0x40, 0xa0);

  public static final Color RED_C = new Color(0xffff6060);
  public static final Color YELLOW_C = new Color(0xffffff00);
  public static final Color GREEN_C = new Color(0xff40ff40);

  private static final float SAMPLE_RATE = 22050;
  private static final int DL_BUFFER_SIZE = 2200;

  private MouseMotionAdapter mouseMotionListener;
  private MouseAdapter mouseListener;

  private SerialMon serial;
  Beeper beeper;

  private ESBNode node;
  private boolean buttonDown = false;
  private boolean resetDown = false;

  private TargetDataLine inDataLine;

  public ESBGui(ESBNode esbNode) {
      super("images/esb.jpg");
      this.node = esbNode;
  }

  @Override
  protected void startGUI() {
    mouseMotionListener = new MouseMotionAdapter() {
      public void mouseMoved(MouseEvent e) {
        //    System.out.println("Mouse moved: " + e.getX() + "," + e.getY());
        int x = e.getX();
        int y = e.getY();
        node.setPIR(x > 18 && x < 80 && y > 35 && y < 100);
        node.setVIB(x > 62 && x < 95 && y > 160 && y < 178);
      }
    };
    addMouseMotionListener(mouseMotionListener);

    mouseListener = new MouseAdapter() {
      // For the button sensor on the ESB nodes.
      public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
          int x = e.getX();
          int y = e.getY();
//        System.err.println("PRESSED AT " + x + "," + y
//                + "  IMAGE=" + getNodeImage().getIconWidth()
//                + "x" + getNodeImage().getIconHeight() +
//                "  SIZE=" + getWidth() + "x" + getHeight());

          if (y > 152 && y < 168) {
            if (x > 0 && x < 19) {
              buttonDown = true;
              node.setButton(true);
            } else {
              int w = getNodeImage().getIconWidth();
              if (x > w - 20 && x < w) {
                resetDown = true;
              }
            }
          }
        }
      }

      public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
          if (buttonDown) {
            buttonDown = false;
            node.setButton(false);
          } else if (resetDown) {
            int x = e.getX();
            int y = e.getY();
            if (y > 152 && y < 168) {
              int w = getNodeImage().getIconWidth();
              if (x > w - 20 && x < w) {
                node.getCPU().reset();
              }
            }
            resetDown = false;
          }
        }
      }
    };
    addMouseListener(mouseListener);

    // Add some windows for listening to serial output
    MSP430 cpu = node.getCPU();
    IOUnit usart = cpu.getIOUnit("USART 1");
    if (usart instanceof USART) {
      if (serial == null) {
        serial = new SerialMon((USART)usart, "RS232 Port Output");
      }
      ((USART) usart).setUSARTListener(serial);
    }

    IOUnit adc = cpu.getIOUnit("ADC12");
    if (adc instanceof ADC12) {
      ((ADC12) adc).setADCInput(0, this);
    }

    beeper = new Beeper();

    // Just a test... TODO: remove!!!
    try {
      AudioFormat af = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
      DataLine.Info dlin =
          new DataLine.Info(TargetDataLine.class, af, DL_BUFFER_SIZE);
      inDataLine = (TargetDataLine) AudioSystem.getLine(dlin);

      if (inDataLine == null) {
        System.out.println("No input dataline");
      } else {
        System.out.println("Format: " + inDataLine.getFormat());
        inDataLine.open(inDataLine.getFormat(), DL_BUFFER_SIZE);
        inDataLine.start();
      }
    } catch (Exception e) {
      System.err.println("Failed to get audio data line: " + e.getMessage());
    }
  }

  @Override
  protected void stopGUI() {
      removeMouseMotionListener(mouseMotionListener);
      removeMouseListener(mouseListener);

      // TODO cleanup
  }

  public void ledsChanged() {
      repaint(LED_BOUNDS);
  }

  private byte[] data = new byte[4];
  public int nextData() {
    if (inDataLine != null) {
      inDataLine.read(data, 0, 4);
    }
    //System.out.println("sampled: " + ((data[1] << 8) + data[0]));
    return (((data[1] & 0xff) << 8) | data[0] & 0xff) >> 4;
  }
  
  protected void paintComponent(Graphics g) {
    Color old = g.getColor();
    int w = getWidth(), h = getHeight();
    ImageIcon esbImage = getNodeImage();
    int iw = esbImage.getIconWidth(), ih = esbImage.getIconHeight();
    esbImage.paintIcon(this, g, 0, 0);
    // Clear all areas not covered by the image
    g.setColor(getBackground());
    if (w > iw) {
      g.fillRect(iw, 0, w, h);
    }
    if (h > ih) {
      g.fillRect(0, ih, w, h);
    }

    // Display all active leds
    if (node.greenLed) {
      g.setColor(GREEN_TRANS);
      g.fillOval(GREEN_X - 1, LED_Y - 3, 5, 9);
      g.setColor(GREEN_C);
      g.fillOval(GREEN_X, LED_Y, 3, 4);
    }
    if (node.redLed) {
      g.setColor(RED_TRANS);
      g.fillOval(RED_X - 1, LED_Y - 3, 5, 9);
      g.setColor(RED_C);
      g.fillOval(RED_X, LED_Y, 3, 4);
    }
    if (node.yellowLed) {
      g.setColor(YELLOW_TRANS);
      g.fillOval(YELLOW_X - 1, LED_Y - 3, 5, 9);
      g.setColor(YELLOW_C);
      g.fillOval(YELLOW_X, LED_Y, 3, 4);
    }
    g.setColor(old);
  }

}
