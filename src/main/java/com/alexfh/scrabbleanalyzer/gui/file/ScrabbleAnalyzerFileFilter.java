package com.alexfh.scrabbleanalyzer.gui.file;

import javax.swing.filechooser.FileFilter;
import java.io.File;

public class ScrabbleAnalyzerFileFilter extends FileFilter
{

    public static final ScrabbleAnalyzerFileFilter INSTANCE  = new ScrabbleAnalyzerFileFilter();
    public static final String                     EXTENSION = ".sa";

    @Override
    public boolean accept(File f)
    {
        return !f.getName().contains(".") || f.getName().substring(f.getName().lastIndexOf(".")).equals(EXTENSION);
    }

    @Override
    public String getDescription()
    {
        return "ScrabbleAnalyzer (" + ScrabbleAnalyzerFileFilter.EXTENSION + ")";
    }

}
