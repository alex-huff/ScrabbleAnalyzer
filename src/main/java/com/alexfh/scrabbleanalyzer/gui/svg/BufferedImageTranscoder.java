package com.alexfh.scrabbleanalyzer.gui.svg;

import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.w3c.dom.Document;

import java.awt.image.BufferedImage;

public class BufferedImageTranscoder extends ImageTranscoder {

    public static final BufferedImageTranscoder INSTANCE = new BufferedImageTranscoder();

    public BufferedImage loadImage(Document svgDocument, float width, float height) {
        this.addTranscodingHint(PNGTranscoder.KEY_WIDTH, width);
        this.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, height);

        try {
            TranscoderInput input = new TranscoderInput(svgDocument);

            this.transcode(input, null);
        } catch (TranscoderException e){
            e.printStackTrace();
            System.exit(-1);
        }

        return this.getBufferedImage();
    }

    private BufferedImage image;

    @Override
    public BufferedImage createImage(int w, int h) {
        return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    }

    @Override
    public void writeImage(BufferedImage img, TranscoderOutput output) {
        this.image = img;
    }

    public BufferedImage getBufferedImage() {
        return image;
    }

}